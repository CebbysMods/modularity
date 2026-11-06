package lv.cebbys.mcmods.modularity.platform.compat;

import net.minecraft.core.PositionAndRotation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.PositionPath;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Entity base for entities that interpolate client positions themselves: maps the server updates to {@link #onLerpTo}.
 * 26.3: the handler is an interface created by {@code createInterpolationHandler}; ours forwards the end position of
 * each update and never interpolates itself.
 */
public abstract class InterpolatedEntity extends BoundsEntity {

    protected InterpolatedEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    /** A position update from the server, to reach in {@code steps} ticks. Client only. */
    protected abstract void onLerpTo(double x, double y, double z, float yRot, float xRot, int steps);

    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return new InterpolationHandler() {
            @Override
            public PositionAndRotation target() {
                return PositionAndRotation.of(position(), getYRot(), getXRot());
            }

            @Override
            public boolean interpolateTo(PositionPath path, float yRot, float xRot, boolean rotate) {
                Vec3 position = path.endPosition();
                onLerpTo(position.x, position.y, position.z, yRot, xRot,
                        LinearInterpolationHandler.DEFAULT_INTERPOLATION_STEPS);
                return true;
            }

            @Override
            public void interpolate() {
            }

            @Override
            public void applyPredictedMovement(Vec3 movement) {
            }

            @Override
            public boolean hasActiveInterpolation() {
                return false;
            }

            @Override
            public void cancel() {
            }
        };
    }
}
