package lv.cebbys.mcmods.modularity.api.entity;

import lv.cebbys.mcmods.modularity.api.core.InteractionResult;
import lv.cebbys.mcmods.modularity.api.world.Player;

import java.util.Optional;

/** Logic of an entity you define. Every method has a do-nothing default. */
public interface EntityBehaviour {
    EntityBehaviour NONE = new EntityBehaviour() {
    };

    /** Called every tick on both sides. */
    default void tick(EntityContext ctx) {
    }

    /** A player right-clicks the entity. */
    default InteractionResult interact(EntityContext ctx, Player player) {
        return InteractionResult.PASS;
    }

    /**
     * The entity is attacked. Return {@code true} when handled (the entity decides itself whether
     * to remove itself).
     */
    default boolean attacked(EntityContext ctx, Optional<EntityRef> attacker, float amount) {
        return false;
    }
}
