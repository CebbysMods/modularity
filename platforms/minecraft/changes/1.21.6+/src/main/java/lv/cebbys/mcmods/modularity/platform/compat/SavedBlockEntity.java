package lv.cebbys.mcmods.modularity.platform.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Block entity base: maps saving and loading to one compound under {@link SavedEntity#DATA_KEY}. 1.21.6: through
 * {@code ValueInput}/{@code ValueOutput}; the compound is stored with its codec, same format.
 */
public abstract class SavedBlockEntity extends BlockEntity {

    protected SavedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Loads the block entity's data (also when an update arrives on the client); empty when nothing was saved. */
    protected abstract void loadData(CompoundTag data);

    /** The block entity's data to save and send. */
    protected abstract CompoundTag saveData();

    @Override
    protected final void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        loadData(input.read(SavedEntity.DATA_KEY, CompoundTag.CODEC).orElseGet(CompoundTag::new));
    }

    @Override
    protected final void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store(SavedEntity.DATA_KEY, CompoundTag.CODEC, saveData());
    }
}
