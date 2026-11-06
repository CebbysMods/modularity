package lv.cebbys.mcmods.modularity.platform.compat;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Entity base: maps a player using (right-clicking) the entity to {@link #onInteract}. */
public abstract class UsableEntity extends DamageEntity {

    protected UsableEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    /** A player uses this entity with {@code hand}. */
    protected abstract InteractionResult onInteract(Player player, InteractionHand hand);

    @Override
    public final InteractionResult interact(Player player, InteractionHand hand) {
        return onInteract(player, hand);
    }
}
