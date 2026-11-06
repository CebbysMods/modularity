package lv.cebbys.mcmods.modularity.platform.loader;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

/**
 * Block entity render data for models, from any block getter: the level, or a chunk region while chunks mesh off
 * the game thread (where each loader keeps its own snapshot).
 */
public interface RenderDataAccess {

    /** The render data of the block entity at {@code pos}, or null. */
    Object renderData(BlockGetter getter, BlockPos pos);
}
