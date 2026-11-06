package lv.cebbys.mcmods.modularity.fabric.compat;

import net.minecraft.client.KeyMapping;

/** Key mappings of a mod, in the mod's own category. Only loaded on the client. */
public final class KeyMappings {
    private KeyMappings() {
    }

    /** An (unregistered) key mapping with translation key {@code key.<modId>.<name>}. */
    public static KeyMapping create(String modId, String name, int defaultKey) {
        return new KeyMapping("key." + modId + "." + name, defaultKey, "key.categories." + modId);
    }
}
