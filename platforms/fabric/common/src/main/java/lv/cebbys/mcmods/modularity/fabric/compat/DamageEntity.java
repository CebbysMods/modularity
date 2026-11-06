package lv.cebbys.mcmods.modularity.fabric.compat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * First link of the Modularity entity bases (damage, then {@link BoundsEntity}, then {@link InterpolatedEntity}); one
 * concern per class, so a version layer replaces only the link that changed. Maps the damage entry point to
 * {@link #onHurt}.
 */
public abstract class DamageEntity extends Entity {

    protected DamageEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    /** Damage that is not blocked by invulnerability; on both sides. Returns whether it was taken. */
    protected abstract boolean onHurt(DamageSource source, float amount);

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !isInvulnerableTo(source) && onHurt(source, amount);
    }
}
