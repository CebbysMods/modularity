package lv.cebbys.mcmods.modularity.fabric.loader;

import lv.cebbys.mcmods.modularity.platform.loader.RenderDataAccess;
import lv.cebbys.mcmods.modularity.fabric.compat.BlockViews;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

/** Render data through Fabric's block view API, which snapshots it for off-thread chunk meshing. */
public final class FabricRenderData implements RenderDataAccess {
    @Override
    public Object renderData(BlockGetter getter, BlockPos pos) {
        return BlockViews.renderData(getter, pos);
    }
}
