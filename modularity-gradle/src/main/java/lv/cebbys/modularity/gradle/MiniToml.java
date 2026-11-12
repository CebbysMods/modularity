package lv.cebbys.modularity.gradle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The TOML subset used by {@code gradle/platforms.toml}: {@code [table]}, {@code [a.b]}, {@code [[array]]},
 * {@code key = "string" | integer | true/false | ["string", ...]} and {@code #} comments. Keeps the plugin free of
 * extra dependencies.
 */
final class MiniToml {

    private MiniToml() {
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> parse(String text) {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> current = root;
        int lineNo = 0;
        for (String raw : text.split("\r?\n")) {
            lineNo++;
            String line = stripComment(raw).trim();
            if (line.isEmpty()) {
                continue;
            }
            try {
                if (line.startsWith("[[")) {
                    if (!line.endsWith("]]")) {
                        throw new IllegalArgumentException("unclosed [[");
                    }
                    String[] path = line.substring(2, line.length() - 2).trim().split("\\.");
                    Map<String, Object> parent = table(root, path, path.length - 1);
                    List<Map<String, Object>> list = (List<Map<String, Object>>) parent
                            .computeIfAbsent(path[path.length - 1], k -> new ArrayList<Map<String, Object>>());
                    current = new LinkedHashMap<>();
                    list.add(current);
                } else if (line.startsWith("[")) {
                    if (!line.endsWith("]")) {
                        throw new IllegalArgumentException("unclosed [");
                    }
                    String[] path = line.substring(1, line.length() - 1).trim().split("\\.");
                    current = table(root, path, path.length);
                } else {
                    int eq = line.indexOf('=');
                    if (eq <= 0) {
                        throw new IllegalArgumentException("expected key = value");
                    }
                    String key = unquote(line.substring(0, eq).trim());
                    if (current.containsKey(key)) {
                        throw new IllegalArgumentException("duplicate key '" + key + "'");
                    }
                    current.put(key, value(line.substring(eq + 1).trim()));
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("TOML line " + lineNo + ": " + e.getMessage() + ": " + raw, e);
            }
        }
        return root;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> table(Map<String, Object> root, String[] path, int depth) {
        Map<String, Object> t = root;
        for (int i = 0; i < depth; i++) {
            Object next = t.computeIfAbsent(unquote(path[i].trim()), k -> new LinkedHashMap<String, Object>());
            if (next instanceof List<?> list) {
                next = list.get(list.size() - 1);
            }
            if (!(next instanceof Map)) {
                throw new IllegalArgumentException("'" + path[i] + "' is not a table");
            }
            t = (Map<String, Object>) next;
        }
        return t;
    }

    private static Object value(String v) {
        if (v.startsWith("\"")) {
            if (v.length() < 2 || !v.endsWith("\"")) {
                throw new IllegalArgumentException("unclosed string");
            }
            return v.substring(1, v.length() - 1);
        }
        if (v.startsWith("[")) {
            if (!v.endsWith("]")) {
                throw new IllegalArgumentException("unclosed array");
            }
            List<Object> list = new ArrayList<>();
            String inner = v.substring(1, v.length() - 1).trim();
            if (!inner.isEmpty()) {
                for (String item : inner.split(",")) {
                    if (!item.isBlank()) {
                        list.add(value(item.trim()));
                    }
                }
            }
            return list;
        }
        if (v.equals("true") || v.equals("false")) {
            return Boolean.parseBoolean(v);
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("unsupported value '" + v + "'");
        }
    }

    private static String unquote(String key) {
        return key.length() >= 2 && key.startsWith("\"") && key.endsWith("\"") ? key.substring(1, key.length() - 1) : key;
    }

    private static String stripComment(String line) {
        boolean inString = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inString = !inString;
            } else if (c == '#' && !inString) {
                return line.substring(0, i);
            }
        }
        return line;
    }
}
