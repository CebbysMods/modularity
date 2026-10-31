package lv.cebbys.mcmods.modularity.api.entity;

import lv.cebbys.mcmods.modularity.api.core.Id;

/** An entity type you registered. */
public interface EntityHandle {
    Id id();

    EntityDefinition definition();
}
