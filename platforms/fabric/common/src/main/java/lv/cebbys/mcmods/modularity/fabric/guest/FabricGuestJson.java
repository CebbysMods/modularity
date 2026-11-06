package lv.cebbys.mcmods.modularity.fabric.guest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lv.cebbys.mcmods.modularity.platform.guest.GuestDescriptor;

import java.util.List;

/** Fabric metadata for a guest mod: its descriptor with the entrypoints as {@code "modularity"} entrypoints. */
final class FabricGuestJson {
    private FabricGuestJson() {
    }

    /** The {@code fabric.mod.json} Fabric would have read if {@code guest} shipped one. */
    static JsonObject of(GuestDescriptor guest) {
        JsonObject json = guest.json();
        JsonObject fabric = new JsonObject();
        fabric.addProperty("schemaVersion", 1);
        fabric.addProperty("id", guest.id());
        fabric.addProperty("version", guest.version());
        for (String key : List.of("name", "description", "license", "icon", "environment")) {
            if (json.has(key)) {
                fabric.add(key, json.get(key));
            }
        }
        for (String key : List.of("authors", "contributors", "contact")) {
            if (json.has(key)) {
                fabric.add(key, json.get(key));
            }
        }
        JsonObject entrypoints = new JsonObject();
        JsonArray modularity = new JsonArray();
        guest.entrypoints().forEach(modularity::add);
        entrypoints.add("modularity", modularity);
        fabric.add("entrypoints", entrypoints);
        fabric.add("depends", GuestDescriptor.object(guest.depends()));
        for (String key : List.of("recommends", "suggests", "breaks", "conflicts")) {
            if (json.has(key)) {
                fabric.add(key, GuestDescriptor.object(guest.map(key)));
            }
        }
        return fabric;
    }
}
