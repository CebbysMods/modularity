package lv.cebbys.mcmods.modularity.platform.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import org.joml.Quaternionf;

/** Serializers for synced entity data. */
public final class SyncedData {
    /** A rotation. */
    public static final EntityDataSerializer<Quaternionf> QUATERNION = EntityDataSerializers.QUATERNION;

    /** A compound tag, copied on set. */
    public static final EntityDataSerializer<CompoundTag> COMPOUND = EntityDataSerializers.COMPOUND_TAG;

    private SyncedData() {
    }

    /** Registers the serializers; called once during mod initialization, on both sides. */
    public static void init() {
    }
}
