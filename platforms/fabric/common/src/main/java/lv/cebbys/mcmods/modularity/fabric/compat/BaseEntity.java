package lv.cebbys.mcmods.modularity.fabric.compat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Base of the Modularity entities: maps the version's damage entry point to {@link #onHurt}. */
public abstract class BaseEntity extends Entity {

    protected BaseEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    /** Damage that is not blocked by invulnerability; on both sides. Returns whether it was taken. */
    protected abstract boolean onHurt(DamageSource source, float amount);

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !isInvulnerableTo(source) && onHurt(source, amount);
    }
}
