package lv.cebbys.mcmods.modularity.fabric.compat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Base of the Modularity entities: maps the version's damage entry point to {@link #onHurt}. */
public abstract class BaseEntity extends Entity {

    protected BaseEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    /** Damage that is not blocked by invulnerability; on both sides. Returns whether it was taken. */
    protected abstract boolean onHurt(DamageSource source, float amount);

    /** The entity's box; {@code vanilla} is the box of its dimensions at its position. */
    protected AABB boundingBox(AABB vanilla) {
        return vanilla;
    }

    @Override
    protected final AABB makeBoundingBox() {
        return boundingBox(super.makeBoundingBox());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !isInvulnerableTo(source) && onHurt(source, amount);
    }
}
