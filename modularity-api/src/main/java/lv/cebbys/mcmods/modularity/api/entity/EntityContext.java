package lv.cebbys.mcmods.modularity.api.entity;

import lv.cebbys.mcmods.modularity.api.world.Level;

/** The entity a behaviour callback is about. */
public interface EntityContext {
    EntityRef entity();

    default Level level() {
        return entity().level();
    }

    default boolean isClientSide() {
        return level().isClientSide();
    }
}
