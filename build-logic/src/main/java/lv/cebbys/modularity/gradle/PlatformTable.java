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
 * @param fabricLoader {@code [defaults] fabric-loader}: the {@code fabricloader} dependency of generated
 *                     {@code fabric.mod.json} files (also on Quilt, which runs the Fabric jar)
 */
public record PlatformTable(Mod mod, List<Platform> platforms, String fabricLoader) {

    public static final String FILE = "gradle/platforms.toml";

    /**
     * One platform: one loader, one Minecraft version, one project ({@code :platforms:<loader>-<name>}).
     *
     * @param loaderVersion Fabric Loader, Quilt Loader, NeoForge or Forge version
     * @param fabricApi     Fabric API version (Fabric and Quilt), else {@code null}
     */
    public record Platform(String name, Loader loader, McVersion minecraft, String loaderVersion, String fabricApi,
                           int java, String modmenu, boolean enabled, boolean smokeClient) {

        /** {@code <loader>-<name>}, e.g. {@code fabric-21.1}: project name and {@code -Pmodularity.platforms} value. */
        public String id() {
            return loader.id() + "-" + name;
        }

        /** Project path, e.g. {@code :platforms:fabric-21.1}. */
        public String projectPath() {
            return ":platforms:" + id();
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
    }

    /** Values for the generated {@code fabric.mod.json}. */
    public record Mod(String id, String name, String description, String license, List<String> authors,
                      String sources, String mixinPackage, Map<String, List<String>> entrypoints,
                      Map<String, String> depends, String archivePrefix, List<String> nested,
                      List<String> mods) {
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
                (List<String>) mod.getOrDefault("nested", List.of()), (List<String>) mod.getOrDefault("mods", List.of()));

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
                    (Boolean) p.getOrDefault("smoke-client", false)));
        }
        platforms.sort(java.util.Comparator.comparing(Platform::loader).thenComparing(Platform::minecraft));
        return new PlatformTable(m, List.copyOf(platforms), (String) defaults.get("fabric-loader"));
    }

    /** The platform with this id ({@code fabric-21.1}). */
    public Optional<Platform> platform(String id) {
        return platforms.stream().filter(p -> p.id().equals(id)).findFirst();
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
