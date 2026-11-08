package lv.cebbys.mcmods.modularity.api.item;

import lv.cebbys.mcmods.modularity.api.core.Id;

public interface ItemHandle {
    Id id();

    ItemDefinition definition();

    /** A new stack of this item. */
    ItemStackRef stack(int count);
}
