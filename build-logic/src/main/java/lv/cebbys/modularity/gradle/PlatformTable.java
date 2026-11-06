package lv.cebbys.modularity.gradle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** {@code gradle/platforms.toml}: the mod metadata and one entry per platform. */
public record PlatformTable(Mod mod, List<Platform> platforms) {

    public static final String FILE = "gradle/platforms.toml";

    /** One Fabric platform: one Minecraft version, one jar. */
    public record Platform(String name, McVersion minecraft, String loader, String fabricApi, int java,
                           String modmenu, boolean enabled) {

        /** Project path, e.g. {@code :platforms:fabric-21.1}. */
        public String projectPath() {
            return ":platforms:fabric-" + name;
        }

        public boolean remap() {
            return minecraft.obfuscated();
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
        List<String> names = new ArrayList<>();
        for (Map<String, Object> p : (List<Map<String, Object>>) toml.getOrDefault("platform", List.of())) {
            String name = str(p, "name", "platform");
            if (names.contains(name)) {
                throw new IllegalArgumentException("Platform " + name + " is listed twice in " + FILE);
            }
            names.add(name);
            McVersion minecraft = McVersion.parse(str(p, "minecraft", "platform " + name));
            if (!McVersion.parseShort(name).equals(minecraft)) {
                throw new IllegalArgumentException("Platform " + name + " does not match minecraft " + minecraft);
            }
            Object loader = p.getOrDefault("loader", defaults.get("loader"));
            if (loader == null) {
                throw new IllegalArgumentException("Platform " + name + ": no loader (and no [defaults] loader)");
            }
            platforms.add(new Platform(name, minecraft, (String) loader, str(p, "fabric-api", "platform " + name),
                    (Integer) require(p, "java", "platform " + name), (String) p.get("modmenu"),
                    (Boolean) p.getOrDefault("enabled", true)));
        }
        platforms.sort((a, b) -> a.minecraft().compareTo(b.minecraft()));
        return new PlatformTable(m, List.copyOf(platforms));
    }

    public Optional<Platform> platform(String name) {
        return platforms.stream().filter(p -> p.name().equals(name)).findFirst();
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
