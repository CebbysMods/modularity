package lv.cebbys.mcmods.modularity.platform.compat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Entity base (chain: see {@link SavedEntity}): maps the damage entry point to {@link #onHurt}.
 */
public abstract class DamageEntity extends SavedEntity {

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
