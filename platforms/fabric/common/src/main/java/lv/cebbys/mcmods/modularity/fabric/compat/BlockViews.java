package lv.cebbys.mcmods.modularity.fabric.compat;

import net.fabricmc.fabric.api.blockview.v2.FabricBlockView;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

/** Reading Fabric's block entity render data from any block getter (level, chunk region, Sodium slice). */
public final class BlockViews {
    private BlockViews() {
    }

    public static Object renderData(BlockGetter getter, BlockPos pos) {
        return ((FabricBlockView) getter).getBlockEntityRenderData(pos);
    }
}
