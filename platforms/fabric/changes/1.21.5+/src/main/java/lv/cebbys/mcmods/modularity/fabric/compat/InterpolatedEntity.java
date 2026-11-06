package lv.cebbys.mcmods.modularity.fabric.compat;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Entity base for entities that interpolate client positions themselves: maps the server updates to {@link #onLerpTo}.
 * 1.21.5: updates go to the entity's {@link InterpolationHandler}; ours forwards them and never interpolates itself.
 */
public abstract class InterpolatedEntity extends BoundsEntity {

    private final InterpolationHandler interpolation = new InterpolationHandler(this) {
        @Override
        public void interpolateTo(Vec3 position, float yRot, float xRot) {
            onLerpTo(position.x, position.y, position.z, yRot, xRot, InterpolationHandler.DEFAULT_INTERPOLATION_STEPS);
        }
    };

    protected InterpolatedEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    /** A position update from the server, to reach in {@code steps} ticks. Client only. */
    protected abstract void onLerpTo(double x, double y, double z, float yRot, float xRot, int steps);

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }
}
