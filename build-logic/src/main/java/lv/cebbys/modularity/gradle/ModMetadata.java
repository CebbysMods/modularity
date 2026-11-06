package lv.cebbys.modularity.gradle;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Generates the mod metadata of one platform: {@code fabric.mod.json} (Fabric, Quilt),
 * {@code META-INF/neoforge.mods.toml} (NeoForge) or {@code META-INF/mods.toml} (Forge), and the mixin config.
 */
public final class ModMetadata {

    private static final Pattern MIXIN = Pattern.compile("^\\s*@Mixin\\s*\\(", Pattern.MULTILINE);

    private ModMetadata() {
    }

    /** Mixin config name in the jar, e.g. {@code modularity.mixins.json}. */
    public static String mixinConfigName(PlatformTable.Mod mod) {
        return mod.id() + ".mixins.json";
    }

    public static String fabricModJson(PlatformTable.Mod mod, PlatformTable.Platform platform, String fabricLoader,
                                       String version, boolean hasMixins) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("schemaVersion", 1);
        json.put("id", mod.id());
        json.put("version", version);
        json.put("name", mod.name());
        json.put("description", mod.description() + " Fabric " + platform.minecraft() + " platform.");
        json.put("authors", mod.authors());
        if (mod.sources() != null) {
            json.put("contact", Map.of("sources", mod.sources()));
        }
        json.put("license", mod.license());
        json.put("environment", "*");
        json.put("entrypoints", mod.entrypoints());
        if (hasMixins) {
            json.put("mixins", List.of(mixinConfigName(mod)));
        }
        Map<String, Object> depends = new LinkedHashMap<>();
        depends.put("fabricloader", ">=" + fabricLoader);
        depends.put("fabric-api", "*");
        depends.putAll(mod.depends());
        // Exact version: each jar is built for one Minecraft version.
        depends.put("minecraft", platform.minecraft().toString());
        depends.put("java", ">=" + platform.java());
        json.put("depends", depends);
        return Json.write(json) + "\n";
    }

    /** Path of the loader metadata file inside the jar. */
    public static String metadataPath(PlatformTable.Platform platform) {
        return switch (platform.loader()) {
            case FABRIC, QUILT -> "fabric.mod.json";
            case NEOFORGE -> "META-INF/neoforge.mods.toml";
            case FORGE -> "META-INF/mods.toml";
        };
    }

    /**
     * {@code neoforge.mods.toml} / {@code mods.toml}: the same mod and dependencies as {@code fabric.mod.json}, with
     * Maven version ranges. Minecraft exact (one jar per version); the loader from its major line on (NeoForge
     * {@code 21.1}, Forge {@code 52}).
     */
    public static String modsToml(PlatformTable.Mod mod, PlatformTable.Platform platform, String version) {
        boolean neo = platform.loader() == Loader.NEOFORGE;
        String[] parts = platform.loaderVersion().split("\\.");
        String loaderLine = neo ? parts[0] + "." + parts[1] : parts[0];
        List<String> lines = new ArrayList<>();
        lines.add("modLoader=\"javafml\"");
        lines.add("loaderVersion=\"[" + (neo ? "1" : parts[0]) + ",)\"");
        lines.add("license=" + quote(mod.license()));
        lines.add("");
        lines.add("[[mods]]");
        lines.add("modId=" + quote(mod.id()));
        lines.add("version=" + quote(version));
        lines.add("displayName=" + quote(mod.name()));
        lines.add("authors=" + quote(String.join(", ", mod.authors())));
        lines.add("description=" + quote(mod.description() + " " + (neo ? "NeoForge " : "Forge ")
                + platform.minecraft() + " platform."));
        Map<String, String> depends = new LinkedHashMap<>();
        depends.put("minecraft", "[" + platform.minecraft() + "]");
        depends.put(neo ? "neoforge" : "forge", "[" + loaderLine + ",)");
        mod.depends().forEach((id, range) -> depends.put(id, mavenRange(range)));
        depends.forEach((id, range) -> {
            lines.add("");
            lines.add("[[dependencies." + mod.id() + "]]");
            lines.add("modId=" + quote(id));
            lines.add(neo ? "type=\"required\"" : "mandatory=true");
            lines.add("versionRange=" + quote(range));
            lines.add("ordering=\"NONE\"");
            lines.add("side=\"BOTH\"");
        });
        return String.join("\n", lines) + "\n";
    }

    /** Fabric-style range of the table ({@code >=4.0.0}, {@code *}, exact) as a Maven range ({@code [4.0.0,)}). */
    static String mavenRange(String range) {
        String r = range.strip();
        if (r.equals("*")) {
            return "[0,)";
        }
        if (r.startsWith(">=")) {
            return "[" + r.substring(2).strip() + ",)";
        }
        if (r.matches("[0-9][0-9A-Za-z.+-]*")) {
            return "[" + r + "]";
        }
        throw new IllegalArgumentException("Range '" + range + "' has no Maven form (use >=X, an exact version or *)");
    }

    private static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /**
     * Mixin config from the {@code @Mixin} classes present in the assembled sources: classes in the {@code client}
     * sub-package go to {@code client}, the rest to {@code mixins}.
     */
    public static String mixinConfig(PlatformTable.Mod mod, PlatformTable.Platform platform, List<String> mixins) {
        List<String> common = new ArrayList<>();
        List<String> client = new ArrayList<>();
        for (String m : mixins) {
            (m.startsWith("client.") ? client : common).add(m);
        }
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("required", true);
        json.put("minVersion", "0.8");
        json.put("package", mod.mixinPackage());
        // JAVA_21 for every platform: the mixins use no newer language features, and every Mixin
        // version shipped with Fabric Loader 0.20 knows it.
        json.put("compatibilityLevel", "JAVA_21");
        json.put("mixins", common);
        json.put("client", client);
        json.put("injectors", Map.of("defaultRequire", 1));
        return Json.write(json) + "\n";
    }

    /** Mixin class names relative to the mixin package, sorted, found in {@code javaDir}. */
    public static List<String> findMixins(Path javaDir, String mixinPackage) {
        Path pkg = javaDir.resolve(mixinPackage.replace('.', '/'));
        if (!Files.isDirectory(pkg)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(pkg)) {
            List<String> result = new ArrayList<>();
            for (Path f : walk.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
                if (MIXIN.matcher(Files.readString(f)).find()) {
                    String rel = pkg.relativize(f).toString().replace('\\', '/');
                    result.add(rel.substring(0, rel.length() - ".java".length()).replace('/', '.'));
                }
            }
            // String order, not path order (case-insensitive on Windows): same config on every OS.
            result.sort(null);
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Minimal pretty JSON writer for maps, lists, strings, numbers and booleans. */
    static final class Json {
        private Json() {
        }

        static String write(Object value) {
            StringBuilder sb = new StringBuilder();
            write(sb, value, "");
            return sb.toString();
        }

        private static void write(StringBuilder sb, Object v, String indent) {
            if (v instanceof Map<?, ?> map) {
                if (map.isEmpty()) {
                    sb.append("{}");
                    return;
                }
                sb.append("{\n");
                int i = 0;
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    sb.append(indent).append("  ");
                    string(sb, e.getKey().toString());
                    sb.append(": ");
                    write(sb, e.getValue(), indent + "  ");
                    sb.append(++i < map.size() ? ",\n" : "\n");
                }
                sb.append(indent).append('}');
            } else if (v instanceof List<?> list) {
                if (list.isEmpty()) {
                    sb.append("[]");
                    return;
                }
                sb.append("[\n");
                for (int i = 0; i < list.size(); i++) {
                    sb.append(indent).append("  ");
                    write(sb, list.get(i), indent + "  ");
                    sb.append(i + 1 < list.size() ? ",\n" : "\n");
                }
                sb.append(indent).append(']');
            } else if (v instanceof String s) {
                string(sb, s);
            } else {
                sb.append(v);
            }
        }

        private static void string(StringBuilder sb, String s) {
            sb.append('"');
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '"' -> sb.append("\\\"");
                    case '\\' -> sb.append("\\\\");
                    case '\n' -> sb.append("\\n");
                    case '\t' -> sb.append("\\t");
                    default -> {
                        if (c < 0x20) {
                            sb.append(String.format("\\u%04x", (int) c));
                        } else {
                            sb.append(c);
                        }
                    }
                }
            }
            sb.append('"');
        }
    }
}
