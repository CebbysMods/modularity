package lv.cebbys.mcmods.modularity.fabric.compat;

import lv.cebbys.mcmods.modularity.api.core.Id;
import net.minecraft.resources.ResourceLocation;

/**
 * Minecraft's namespaced identifiers. The only place (with a few small client classes) that names their type, so
 * the rest of the code passes portable {@link Id}s or uses the result inline.
 */
public final class Ids {
    private Ids() {
    }

    public static ResourceLocation rl(Id id) {
        return ResourceLocation.fromNamespaceAndPath(id.namespace(), id.path());
    }

    public static ResourceLocation of(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public static ResourceLocation parse(String text) {
        return ResourceLocation.parse(text);
    }

    public static Id id(ResourceLocation location) {
        return Id.of(location.getNamespace(), location.getPath());
    }
}
