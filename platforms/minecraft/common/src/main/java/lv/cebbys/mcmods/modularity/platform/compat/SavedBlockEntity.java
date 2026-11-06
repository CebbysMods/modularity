package lv.cebbys.mcmods.modularity.platform.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Block entity base: maps saving and loading to one compound under {@link SavedEntity#DATA_KEY}. */
public abstract class SavedBlockEntity extends BlockEntity {

    protected SavedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Loads the block entity's data (also when an update arrives on the client); empty when nothing was saved. */
    protected abstract void loadData(CompoundTag data);

    /** The block entity's data to save and send. */
    protected abstract CompoundTag saveData();

    @Override
    protected final void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadData(Nbt.compound(tag, SavedEntity.DATA_KEY));
    }

    @Override
    protected final void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(SavedEntity.DATA_KEY, saveData());
    }
}
