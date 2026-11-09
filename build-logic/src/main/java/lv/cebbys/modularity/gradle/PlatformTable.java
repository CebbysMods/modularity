package lv.cebbys.modularity.gradle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code gradle/platforms.toml}: the mod metadata and one entry per platform.
 *
 * @param fabricLoader    {@code [defaults] fabric-loader}: the Fabric Loader the Fabric platforms build and run with
 * @param fabricLoaderMin {@code [defaults] fabric-loader-min} (default: {@code fabricLoader}): the {@code fabricloader}
 *                        dependency of generated {@code fabric.mod.json} files; Quilt Loader reports an older
 *                        Fabric Loader version, and the Fabric jar must load there too
 */
public record PlatformTable(Mod mod, List<Platform> platforms, String fabricLoader, String fabricLoaderMin) {

    public static final String FILE = "gradle/platforms.toml";

    /**
     * One platform: one loader, one Minecraft version, one project
     * ({@code :platforms:<name>:<name>-<loader>}).
     *
     * @param loaderVersion Fabric Loader, Quilt Loader, NeoForge or Forge version
     * @param fabricApi     Fabric API version (Fabric and Quilt), else {@code null}
     * @param parchment     Parchment version of this Minecraft version ({@code [parchment]} in the table), else
     *                      {@code null}: official Mojang mappings alone
     */
    public record Platform(String name, Loader loader, McVersion minecraft, String loaderVersion, String fabricApi,
                           int java, String modmenu, boolean enabled, boolean smokeClient, String parchment) {

        /** {@code <loader>-<name>}, e.g. {@code fabric-21.1}: selection id and {@code -Pmodularity.platforms} value. */
        public String id() {
            return loader.id() + "-" + name;
        }

        /** Gradle project name, version first, e.g. {@code 21.1-fabric}. */
        public String projectName() {
            return name + "-" + loader.id();
        }

        /** Parent project of every loader of this version, e.g. {@code :platforms:21.1}. */
        public String parentPath() {
            return ":platforms:" + name;
        }

        /** Directory of the parent project, e.g. {@code platforms/versions/21.1}. */
        public String parentDir() {
            return "platforms/versions/" + name;
        }

        /** Project path, e.g. {@code :platforms:21.1:21.1-fabric}. */
        public String projectPath() {
            return parentPath() + ":" + projectName();
        }

        /** Project dir relative to the root, e.g. {@code platforms/fabric/versions/21.1}. */
        public String projectDir() {
            return "platforms/" + loader.id() + "/versions/" + name;
        }

        /** Obfuscated Minecraft that Fabric/Quilt must remap (NeoForge and Forge run with Mojang names). */
        public boolean remap() {
            return (loader == Loader.FABRIC || loader == Loader.QUILT) && minecraft.obfuscated();
        }

        public boolean fabricLike() {
            return loader == Loader.FABRIC || loader == Loader.QUILT;
        }

        /**
         * {@code org.parchmentmc.data:parchment-<minecraft>:<parchment>@zip}, the layered-mapping notation of Loom;
         * {@code null} without Parchment (official Mojang mappings alone).
         */
        public String parchmentArtifact() {
            return parchment == null ? null
                    : "org.parchmentmc.data:parchment-" + minecraft + ":" + parchment + "@zip";
        }
    }

    /**
     * Values for the generated {@code fabric.mod.json}.
     *
     * @param companionMods the mods that sit next to Modularity, named once and without a loader
     *                      ({@code :utilities:mvd:mvd}); {@link #companionProject} gives the build of one
     */
    public record Mod(String id, String name, String description, String license, List<String> authors,
                      String sources, String mixinPackage, Map<String, List<String>> entrypoints,
                      Map<String, String> depends, String archivePrefix, List<String> nested,
                      List<String> companionMods, String icon) {

        /** Jar path of the icon ({@code assets/<id>/icon.png}), or null without {@code icon}. */
        public String iconPath() {
            return icon == null ? null : "assets/" + id + "/icon.png";
        }

        /** The loader's build of a companion mod: {@code :utilities:mvd:mvd} and {@code NEOFORGE} give {@code -neoforge}. */
        public static String companionProject(String companionMod, Loader loader) {
            return companionMod + "-" + loader.id();
        }

        /**
         * The loader independent API of a companion mod ({@code :utilities:mvd:mvd} gives {@code -core}): the NeoForge
         * and Forge builds pack it inside and do not export it, as its packages would otherwise be split between two
         * module jars at run time.
         */
        public static String companionApi(String companionMod) {
            return companionMod + "-core";
        }
    }

    public static PlatformTable read(Path file) {
        try {
            return parse(Files.readString(file));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + file, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static PlatformTable parse(String text) {
        Map<String, Object> toml = MiniToml.parse(text);
        Map<String, Object> defaults = (Map<String, Object>) toml.getOrDefault("defaults", Map.of());
        Map<String, Object> mod = (Map<String, Object>) require(toml, "mod", "");

        Map<String, List<String>> entrypoints = new LinkedHashMap<>();
        ((Map<String, Object>) mod.getOrDefault("entrypoints", Map.of()))
                .forEach((k, v) -> entrypoints.put(k, (List<String>) v));
        Map<String, String> depends = new LinkedHashMap<>();
        ((Map<String, Object>) mod.getOrDefault("depends", Map.of())).forEach((k, v) -> depends.put(k, (String) v));

        Mod m = new Mod(str(mod, "id", "mod"), str(mod, "name", "mod"), str(mod, "description", "mod"),
                str(mod, "license", "mod"), (List<String>) require(mod, "authors", "mod"),
                (String) mod.get("sources"), str(mod, "mixin-package", "mod"), entrypoints, depends,
                (String) mod.getOrDefault("archive-prefix", mod.get("id")),
                (List<String>) mod.getOrDefault("nested", List.of()),
                (List<String>) mod.getOrDefault("companion-mods", List.of()),
                (String) mod.get("icon"));

        // Parchment is published per Minecraft version, not per loader: one entry applies to every platform of it.
        Map<String, Object> parchment = (Map<String, Object>) toml.getOrDefault("parchment", Map.of());

        List<Platform> platforms = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (Map<String, Object> p : (List<Map<String, Object>>) toml.getOrDefault("platform", List.of())) {
            String name = str(p, "name", "platform");
            Loader loader = Loader.parse((String) p.getOrDefault("loader", "fabric"));
            String where = "platform " + loader.id() + "-" + name;
            if (ids.contains(loader.id() + "-" + name)) {
                throw new IllegalArgumentException("Platform " + loader.id() + "-" + name + " is listed twice in " + FILE);
            }
            ids.add(loader.id() + "-" + name);
            McVersion minecraft = McVersion.parse(str(p, "minecraft", where));
            if (!McVersion.parseShort(name).equals(minecraft)) {
                throw new IllegalArgumentException("Platform " + name + " does not match minecraft " + minecraft);
            }
            // Fabric/Quilt Loader: one version for all platforms ([defaults]); NeoForge/Forge: per platform.
            String versionKey = switch (loader) {
                case FABRIC -> "fabric-loader";
                case QUILT -> "quilt-loader";
                case NEOFORGE -> "neoforge";
                case FORGE -> "forge";
            };
            Object loaderVersion = p.getOrDefault(versionKey, defaults.get(versionKey));
            if (loaderVersion == null) {
                throw new IllegalArgumentException(where + ": no '" + versionKey + "' (here or in [defaults])");
            }
            String fabricApi = loader == Loader.FABRIC || loader == Loader.QUILT ? str(p, "fabric-api", where) : null;
            platforms.add(new Platform(name, loader, minecraft, (String) loaderVersion, fabricApi,
                    (Integer) require(p, "java", where), (String) p.get("modmenu"),
                    (Boolean) p.getOrDefault("enabled", true),
                    (Boolean) p.getOrDefault("smoke-client", false),
                    (String) parchment.get(minecraft.toString())));
        }
        for (String version : parchment.keySet()) {
            if (platforms.stream().noneMatch(p -> p.minecraft().toString().equals(version))) {
                throw new IllegalArgumentException("[parchment] " + version + ": no platform of that Minecraft version in "
                        + FILE);
            }
        }
        platforms.sort(java.util.Comparator.comparing(Platform::loader).thenComparing(Platform::minecraft));
        String fabricLoader = (String) defaults.get("fabric-loader");
        return new PlatformTable(m, List.copyOf(platforms), fabricLoader,
                (String) defaults.getOrDefault("fabric-loader-min", fabricLoader));
    }

    /** The platform with this id ({@code fabric-21.1}). */
    public Optional<Platform> platform(String id) {
        return platforms.stream().filter(p -> p.id().equals(id)).findFirst();
    }

    /**
     * Any platform of this Minecraft version ({@code 21.1}): the version parent. Every loader of a version shares
     * the Minecraft version and it is the first one in table order, so this is stable.
     */
    public Optional<Platform> version(String name) {
        return platforms.stream().filter(p -> p.name().equals(name)).findFirst();
    }

    /** The platform with this Gradle project name ({@code 21.1-fabric}). */
    public Optional<Platform> platformByProjectName(String projectName) {
        return platforms.stream().filter(p -> p.projectName().equals(projectName)).findFirst();
    }

    /** The parent projects of every platform ({@code :platforms:21.1}), in table order. */
    public List<String> parentPaths() {
        return platforms.stream().map(Platform::parentPath).distinct().toList();
    }

    public List<Platform> enabled() {
        return platforms.stream().filter(Platform::enabled).toList();
    }

    private static Object require(Map<String, Object> map, String key, String where) {
        Object v = map.get(key);
        if (v == null) {
            throw new IllegalArgumentException("Missing '" + key + "'" + (where.isEmpty() ? "" : " in " + where)
                    + " (" + FILE + ")");
        }
        return v;
    }

    private static String str(Map<String, Object> map, String key, String where) {
        return (String) require(map, key, where);
    }
}
