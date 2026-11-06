package lv.cebbys.mcmods.modularity.fabric.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Entity base: maps "other entities collide with this one" to {@link #isSolid}. 1.21.6: the check gets the other entity. */
public abstract class SolidEntity extends DamageEntity {

    protected SolidEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    /** Whether other entities collide with this one like with a block. */
    protected boolean isSolid() {
        return false;
    }

    @Override
    public final boolean canBeCollidedWith(Entity other) {
        return isSolid();
    }
}
