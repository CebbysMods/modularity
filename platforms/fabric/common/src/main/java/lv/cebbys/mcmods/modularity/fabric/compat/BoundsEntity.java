package lv.cebbys.mcmods.modularity.fabric.compat;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Entity base: maps the bounding box computation to {@link #boundingBox}. */
public abstract class BoundsEntity extends SolidEntity {

    protected BoundsEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    /** The entity's box; {@code vanilla} is the box of its dimensions at its position. */
    protected AABB boundingBox(AABB vanilla) {
        return vanilla;
    }

    @Override
    protected final AABB makeBoundingBox() {
        return boundingBox(super.makeBoundingBox());
    }
}
