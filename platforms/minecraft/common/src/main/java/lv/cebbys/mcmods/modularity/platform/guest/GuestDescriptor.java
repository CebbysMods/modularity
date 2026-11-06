package lv.cebbys.mcmods.modularity.platform.guest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@code modularity.mod.json} of a guest mod: a mod written only against the Modularity API, shipped as one jar with
 * no loader metadata. Modularity turns it into a mod of the running loader (the loader layer writes that loader's
 * metadata from it, e.g. {@code FabricGuestJson}).
 *
 * <pre>{@code
 * {
 *   "id": "refabslab", "version": "1.1.0", "name": "Refabricated Slabs", "description": "...",
 *   "authors": ["CebbyS"], "contact": {"homepage": "..."}, "license": "CC0-1.0", "icon": "assets/refabslab/icon.png",
 *   "entrypoints": ["lv.cebbys.mcmods.refabslab.RefabslabMod"],
 *   "minecraft": ">=1.21.1", "modularity": "*",
 *   "depends": {}, "recommends": {}, "suggests": {"kleeslabs": "*"}, "breaks": {}
 * }
 * }</pre>
 */
public record GuestDescriptor(Path origin, JsonObject json) {

    public static final String FILE = "modularity.mod.json";

    public static GuestDescriptor read(Path origin, Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            for (String required : List.of("id", "version", "entrypoints")) {
                if (!json.has(required)) {
                    throw new IOException(origin + ": " + FILE + " has no \"" + required + "\"");
                }
            }
            return new GuestDescriptor(origin, json);
        }
    }

    public String id() {
        return json.get("id").getAsString();
    }

    public String version() {
        return json.get("version").getAsString();
    }

    public List<String> entrypoints() {
        List<String> classes = new ArrayList<>();
        JsonElement e = json.get("entrypoints");
        if (e.isJsonArray()) {
            e.getAsJsonArray().forEach(c -> classes.add(c.getAsString()));
        } else {
            classes.add(e.getAsString());
        }
        return classes;
    }

    /** {@code depends} plus the Minecraft and Modularity ranges. */
    public Map<String, String> depends() {
        var all = new java.util.LinkedHashMap<String, String>();
        all.put("minecraft", string("minecraft", "*"));
        all.put("modularity", string("modularity", "*"));
        all.putAll(map("depends"));
        return all;
    }

    public Map<String, String> map(String key) {
        var out = new java.util.LinkedHashMap<String, String>();
        if (json.has(key)) {
            json.getAsJsonObject(key).entrySet().forEach(e -> out.put(e.getKey(), e.getValue().getAsString()));
        }
        return out;
    }

    private String string(String key, String fallback) {
        return json.has(key) ? json.get(key).getAsString() : fallback;
    }

    /** A string map as a JSON object. */
    public static JsonObject object(Map<String, String> map) {
        JsonObject o = new JsonObject();
        map.forEach(o::addProperty);
        return o;
    }
}
