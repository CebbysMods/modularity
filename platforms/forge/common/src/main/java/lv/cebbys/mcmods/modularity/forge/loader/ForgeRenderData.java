package lv.cebbys.mcmods.modularity.forge.loader;

import lv.cebbys.mcmods.modularity.platform.loader.RenderDataAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

/**
 * The Forge render data seam. Forge has no block view that snapshots render data for off-thread meshing, so this
 * returns the block entity of the asked position, which is the same object Fabric's block view reports on the game
 * thread. The caller must not use it from another thread.
 */
public final class ForgeRenderData implements RenderDataAccess {

    @Override
    public Object renderData(BlockGetter getter, BlockPos pos) {
        return getter.getBlockEntity(pos);
    }
}
