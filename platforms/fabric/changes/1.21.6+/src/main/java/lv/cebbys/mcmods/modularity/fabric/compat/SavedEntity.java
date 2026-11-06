package lv.cebbys.mcmods.modularity.fabric.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * First link of the Modularity entity bases ({@code SavedEntity}, {@link DamageEntity}, {@link UsableEntity},
 * {@link SolidEntity}, {@link BoundsEntity}, {@link InterpolatedEntity}); one concern per class, so a version layer replaces only the
 * link that changed. Maps saving and loading to one compound under {@link #DATA_KEY}. 1.21.6: save data is read and
 * written through {@code ValueInput}/{@code ValueOutput}; the compound is stored with its codec, same format.
 */
public abstract class SavedEntity extends Entity {
    /** Key of the entity's own data in its save data. */
    public static final String DATA_KEY = "modularity";

    protected SavedEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    /** Loads the entity's data; {@code data} is empty when nothing was saved. */
    protected void loadData(CompoundTag data) {
    }

    /** The entity's data to save, or {@code null} for none. */
    protected CompoundTag saveData() {
        return null;
    }

    @Override
    protected final void readAdditionalSaveData(ValueInput input) {
        loadData(input.read(DATA_KEY, CompoundTag.CODEC).orElseGet(CompoundTag::new));
    }

    @Override
    protected final void addAdditionalSaveData(ValueOutput output) {
        CompoundTag data = saveData();
        if (data != null) {
            output.store(DATA_KEY, CompoundTag.CODEC, data);
        }
    }
}
