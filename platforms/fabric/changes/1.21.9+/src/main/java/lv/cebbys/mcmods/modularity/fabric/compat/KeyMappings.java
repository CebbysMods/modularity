package lv.cebbys.mcmods.modularity.fabric.compat;

import lv.cebbys.mcmods.modularity.platform.compat.Ids;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Key mappings of a mod, in the mod's own category. Only loaded on the client. 1.21.9: categories are registered
 * {@link KeyMapping.Category} objects (translation key {@code key.category.<modId>.keys}), one per mod.
 */
public final class KeyMappings {
    private static final Map<String, KeyMapping.Category> CATEGORIES = new ConcurrentHashMap<>();

    private KeyMappings() {
    }

    /** Registers a key mapping with translation key {@code key.<modId>.<name>}. */
    public static KeyMapping register(String modId, String name, int defaultKey) {
        KeyMapping.Category category = CATEGORIES.computeIfAbsent(modId,
                id -> KeyMapping.Category.register(Ids.of(id, "keys")));
        return KeyBindingHelper.registerKeyBinding(new KeyMapping("key." + modId + "." + name, defaultKey, category));
    }
}
