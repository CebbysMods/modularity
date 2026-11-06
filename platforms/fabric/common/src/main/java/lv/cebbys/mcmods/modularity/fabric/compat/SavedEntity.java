package lv.cebbys.mcmods.modularity.fabric.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * First link of the Modularity entity bases ({@code SavedEntity}, {@link DamageEntity}, {@link UsableEntity},
 * {@link SolidEntity}, {@link BoundsEntity}, {@link InterpolatedEntity}); one concern per class, so a version layer replaces only the
 * link that changed. Maps saving and loading to one compound under {@link #DATA_KEY}.
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
    protected final void readAdditionalSaveData(CompoundTag tag) {
        loadData(Nbt.compound(tag, DATA_KEY));
    }

    @Override
    protected final void addAdditionalSaveData(CompoundTag tag) {
        CompoundTag data = saveData();
        if (data != null) {
            tag.put(DATA_KEY, data);
        }
    }
}
