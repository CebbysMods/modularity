package lv.cebbys.mcmods.modularity.fabric.compat;

import net.minecraft.world.entity.Entity;

/** Calls on existing entities. */
public final class EntityCompat {
    private EntityCompat() {
    }

    /** Places the entity without interpolation, e.g. right after creating it. */
    public static void place(Entity entity, double x, double y, double z, float yaw, float pitch) {
        entity.moveTo(x, y, z, yaw, pitch);
    }
}
