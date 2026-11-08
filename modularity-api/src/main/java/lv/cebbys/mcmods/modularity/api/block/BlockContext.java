package lv.cebbys.mcmods.modularity.api.block;

import lv.cebbys.mcmods.modularity.api.data.BlockData;
import lv.cebbys.mcmods.modularity.api.world.BlockView;
import lv.cebbys.mcmods.modularity.api.world.Pos;

import java.util.Optional;

/** The block a behaviour callback is about. */
public interface BlockContext {
    BlockView view();

    Pos pos();

    BlockStateRef state();

    /** Data attached to this block. */
    <T> Optional<T> data(BlockData<T> data);
}
