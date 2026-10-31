package lv.cebbys.mcmods.modularity.api.world;

import lv.cebbys.mcmods.modularity.api.block.BlockStateRef;
import lv.cebbys.mcmods.modularity.api.core.Handle;
import lv.cebbys.mcmods.modularity.api.data.BlockData;

import java.util.Optional;

/**
 * Read-only view of blocks. On the client this may be a render-thread snapshot (chunk render region,
 * Sodium world slice, ...). Platforms make sure {@link #data} works on every view type, which removes
 * the need for mods to locate chunks themselves.
 */
public interface BlockView extends Handle {
    BlockStateRef blockState(Pos pos);

    /** Data attached to the block at {@code pos}, see {@link BlockData}. */
    <T> Optional<T> data(Pos pos, BlockData<T> data);
}
