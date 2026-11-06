package lv.cebbys.mcmods.modularity.fabric.compat;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Entity base for entities that interpolate client positions themselves: maps the server updates to {@link #onLerpTo}. */
public abstract class InterpolatedEntity extends BoundsEntity {

    protected InterpolatedEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    /** A position update from the server, to reach in {@code steps} ticks. Client only. */
    protected abstract void onLerpTo(double x, double y, double z, float yRot, float xRot, int steps);

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        onLerpTo(x, y, z, yRot, xRot, steps);
    }
}
