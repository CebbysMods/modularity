package lv.cebbys.mcmods.modularity.fabric.compat;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

/** Key mappings of a mod, in the mod's own category. Only loaded on the client. */
public final class KeyMappings {
    private KeyMappings() {
    }

    /** Registers a key mapping with translation key {@code key.<modId>.<name>}. */
    public static KeyMapping register(String modId, String name, int defaultKey) {
        return KeyBindingHelper.registerKeyBinding(new KeyMapping("key." + modId + "." + name, defaultKey, "key.categories." + modId));
    }
}
