package lv.cebbys.mcmods.modularity.fabric.compat;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Entity base: maps the bounding box computation to {@link #boundingBox}. 1.21.4: the box is made for a position. */
public abstract class BoundsEntity extends DamageEntity {

    protected BoundsEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    /** The entity's box; {@code vanilla} is the box of its dimensions at its position. */
    protected AABB boundingBox(AABB vanilla) {
        return vanilla;
    }

    @Override
    protected final AABB makeBoundingBox(Vec3 position) {
        return boundingBox(super.makeBoundingBox(position));
    }
}
