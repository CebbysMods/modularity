package lv.cebbys.mcmods.modularity.api.world;

import lv.cebbys.mcmods.modularity.api.block.BlockStateRef;
import lv.cebbys.mcmods.modularity.api.data.BlockData;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;

/** A live world (server or client). */
public interface Level extends BlockView {
    boolean isClientSide();

    /** Whether {@code state} placed at {@code pos} would not collide with entities. */
    boolean isUnobstructed(BlockStateRef state, Pos pos);

    /** Stores data on the block at {@code pos}. The block must declare {@code data} in its definition. */
    <T> void setData(Pos pos, BlockData<T> data, T value);

    /** Drops an item entity at the center of {@code pos}. No-op on the client. */
    void dropItem(Pos pos, ItemStackRef stack);
}
