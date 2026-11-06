package lv.cebbys.mcmods.modularity.fabric.compat;

import net.fabricmc.fabric.api.blockgetter.v2.FabricBlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

/**
 * Reading Fabric's block entity render data from any block getter (level, chunk region, Sodium slice). 26.1:
 * {@code FabricBlockView} is {@code FabricBlockGetter}.
 */
public final class BlockViews {
    private BlockViews() {
    }

    public static Object renderData(BlockGetter getter, BlockPos pos) {
        return ((FabricBlockGetter) getter).getBlockEntityRenderData(pos);
    }
}
