package lv.cebbys.mcmods.modularity.platform.compat;

import lv.cebbys.mcmods.modularity.api.core.Id;
import net.minecraft.resources.Identifier;

/**
 * Minecraft's namespaced identifiers. The only place (with a few small client classes) that names their type, so
 * the rest of the code passes portable {@link Id}s or uses the result inline. 1.21.11: {@code ResourceLocation} is
 * called {@code Identifier}.
 */
public final class Ids {
    private Ids() {
    }

    public static Identifier rl(Id id) {
        return Identifier.fromNamespaceAndPath(id.namespace(), id.path());
    }

    public static Identifier of(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    public static Identifier parse(String text) {
        return Identifier.parse(text);
    }

    public static Id id(Identifier location) {
        return Id.of(location.getNamespace(), location.getPath());
    }
}
