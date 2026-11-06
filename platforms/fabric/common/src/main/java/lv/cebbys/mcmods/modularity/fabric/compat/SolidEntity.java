package lv.cebbys.mcmods.modularity.fabric.compat;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Entity base: maps "other entities collide with this one" to {@link #isSolid}. */
public abstract class SolidEntity extends DamageEntity {

    protected SolidEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    /** Whether other entities collide with this one like with a block. */
    protected boolean isSolid() {
        return false;
    }

    @Override
    public final boolean canBeCollidedWith() {
        return isSolid();
    }
}
