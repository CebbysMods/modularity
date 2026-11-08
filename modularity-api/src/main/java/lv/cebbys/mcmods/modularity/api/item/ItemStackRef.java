package lv.cebbys.mcmods.modularity.api.item;

import lv.cebbys.mcmods.modularity.api.block.BlockRef;
import lv.cebbys.mcmods.modularity.api.core.Handle;
import lv.cebbys.mcmods.modularity.api.core.Id;

import java.util.Optional;

public interface ItemStackRef extends Handle {
    boolean isEmpty();

    int count();

    Id itemId();

    /** The block this item places, when the item is a block item. */
    Optional<BlockRef> block();
}
