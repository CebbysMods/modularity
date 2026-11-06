package lv.cebbys.mcmods.modularity.platform.compat;

import lv.cebbys.mcmods.modularity.platform.mixin.ServerLevelAccessor;
import net.minecraft.world.entity.Entity;

/** Entity ids. 26.2: the id counter belongs to the server level; client entities get their id from the server. */
public final class EntityIds {
    private EntityIds() {
    }

    /**
     * Gives {@code entity} an id followed by {@code parts} free consecutive ids for its parts, like the Ender Dragon:
     * clients build the same parts and both sides resolve part ids from the parent id.
     */
    public static void reserveParts(Entity entity, int parts) {
        if (!entity.level().isClientSide()) {
            entity.setId(ServerLevelAccessor.modularity$counter().getAndAdd(parts + 1) + 1);
        }
    }
}
