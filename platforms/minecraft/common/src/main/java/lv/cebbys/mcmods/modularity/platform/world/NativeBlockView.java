package lv.cebbys.mcmods.modularity.platform.world;

import lv.cebbys.mcmods.modularity.api.block.BlockStateRef;
import lv.cebbys.mcmods.modularity.api.data.BlockData;
import lv.cebbys.mcmods.modularity.api.world.BlockView;
import lv.cebbys.mcmods.modularity.api.world.Pos;
import lv.cebbys.mcmods.modularity.platform.Wrap;
import lv.cebbys.mcmods.modularity.platform.block.ModularBlockEntity;
import lv.cebbys.mcmods.modularity.platform.loader.Loaders;
import net.minecraft.world.level.BlockGetter;

import java.util.Map;
import java.util.Optional;

/**
 * Any {@link BlockGetter}: a level, a chunk render region, Sodium's world slice... Data is read from
 * the block entity when available and from Fabric's block entity render data otherwise, so the
 * same code works on the game thread and while meshing chunks.
 */
public class NativeBlockView implements BlockView {
    protected final BlockGetter getter;

    public NativeBlockView(BlockGetter getter) {
        this.getter = getter;
    }

    @Override
    public BlockStateRef blockState(Pos pos) {
        return Wrap.state(getter.getBlockState(Wrap.pos(pos)));
    }

    @Override
    public <T> Optional<T> data(Pos pos, BlockData<T> data) {
        var nativePos = Wrap.pos(pos);
        if (getter instanceof net.minecraft.world.level.Level
                && getter.getBlockEntity(nativePos) instanceof ModularBlockEntity entity) {
            return entity.get(data);
        }
        Object renderData = Loaders.renderData().renderData(getter, nativePos);
        if (renderData instanceof Map<?, ?> map && map.get(data) != null) {
            @SuppressWarnings("unchecked") T value = (T) map.get(data);
            return Optional.of(value);
        }
        return Optional.empty();
    }

    @Override
    public Object handle() {
        return getter;
    }
}
