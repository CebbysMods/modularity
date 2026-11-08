package lv.cebbys.mcmods.modularity.api.block;

import lv.cebbys.mcmods.modularity.api.core.Handle;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;

/** A registered block type (vanilla, modded or your own). */
public interface BlockRef extends Handle {
    Id id();

    BlockStateRef defaultState();

    /** Coarse vanilla block family check, e.g. {@code block.is(BlockType.SLAB)}. */
    boolean is(BlockType type);

    /** Item form of this block, empty stack when the block has no item. */
    ItemStackRef asStack(int count);
}
