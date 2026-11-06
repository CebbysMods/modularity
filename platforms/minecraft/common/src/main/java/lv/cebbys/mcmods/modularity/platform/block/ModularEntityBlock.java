package lv.cebbys.mcmods.modularity.platform.block;

import lv.cebbys.mcmods.modularity.api.block.BlockDefinition;
import lv.cebbys.mcmods.modularity.api.core.Id;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** A {@link ModularBlock} that stores block data, backed by a {@link ModularBlockEntity}. */
public class ModularEntityBlock extends ModularBlock implements EntityBlock {
    private BlockEntityType<ModularBlockEntity> type;

    protected ModularEntityBlock(Id id, BlockDefinition definition) {
        super(id, definition);
    }

    public void bindType(BlockEntityType<ModularBlockEntity> type) {
        this.type = type;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (type == null) {
            throw new IllegalStateException("Block entity type of " + this + " is not registered");
        }
        return new ModularBlockEntity(type, pos, state);
    }
}
