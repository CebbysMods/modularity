package lv.cebbys.mcmods.modularity.fabric.world;

import lv.cebbys.mcmods.modularity.api.block.BlockStateRef;
import lv.cebbys.mcmods.modularity.api.data.BlockData;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.api.world.Level;
import lv.cebbys.mcmods.modularity.api.world.Pos;
import lv.cebbys.mcmods.modularity.fabric.Wrap;
import lv.cebbys.mcmods.modularity.fabric.block.ModularBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class FabricLevel extends NativeBlockView implements Level {
    private final net.minecraft.world.level.Level level;

    public FabricLevel(net.minecraft.world.level.Level level) {
        super(level);
        this.level = level;
    }

    @Override
    public boolean isClientSide() {
        return level.isClientSide();
    }

    @Override
    public boolean isUnobstructed(BlockStateRef state, Pos pos) {
        return level.isUnobstructed(Wrap.state(state), Wrap.pos(pos), CollisionContext.empty());
    }

    @Override
    public <T> void setData(Pos pos, BlockData<T> data, T value) {
        if (!(level.getBlockEntity(Wrap.pos(pos)) instanceof ModularBlockEntity entity)) {
            throw new IllegalStateException("Block at " + pos + " does not store Modularity block data");
        }
        entity.set(data, value);
    }

    @Override
    public void dropItem(Pos pos, ItemStackRef stack) {
        if (!level.isClientSide() && !stack.isEmpty()) {
            Block.popResource(level, Wrap.pos(pos), Wrap.stack(stack).copy());
        }
    }
}
