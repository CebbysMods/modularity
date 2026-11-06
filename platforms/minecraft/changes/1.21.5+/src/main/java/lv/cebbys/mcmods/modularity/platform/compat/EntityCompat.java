package lv.cebbys.mcmods.modularity.platform.compat;

import net.minecraft.world.entity.Entity;

/** Calls on existing entities. 1.21.5: {@code moveTo} is called {@code snapTo}. */
public final class EntityCompat {
    private EntityCompat() {
    }

    /** Places the entity without interpolation, e.g. right after creating it. */
    public static void place(Entity entity, double x, double y, double z, float yaw, float pitch) {
        entity.snapTo(x, y, z, yaw, pitch);
    }
}
