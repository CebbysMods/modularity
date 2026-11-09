package lv.cebbys.mcmods.modularity.neoforge.loader;

import lv.cebbys.mcmods.modularity.platform.loader.RenderDataAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

/** The NeoForge render data seam. NeoForge keeps no thread-safe block view yet, so the call fails loudly. */
public final class NeoForgeRenderData implements RenderDataAccess {

    @Override
    public Object renderData(BlockGetter getter, BlockPos pos) {
        throw new UnsupportedOperationException(
                "Modularity render data access is not implemented on NeoForge yet (renderData)");
    }
}
