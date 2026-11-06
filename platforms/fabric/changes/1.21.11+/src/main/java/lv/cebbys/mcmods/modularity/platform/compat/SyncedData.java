package lv.cebbys.mcmods.modularity.platform.compat;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricTrackedDataRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import org.joml.Quaternionf;

/**
 * Serializers for synced entity data. 1.21.9: vanilla has no compound serializer any more; ours is registered with
 * Fabric under {@code modularity:compound_tag}, so client and server agree on its id. 1.21.11: vanilla's quaternion
 * serializer is typed {@code Quaternionfc}; ours ({@code modularity:quaternion}) keeps {@code Quaternionf}.
 */
public final class SyncedData {
    /** A rotation. */
    public static final EntityDataSerializer<Quaternionf> QUATERNION = new EntityDataSerializer<>() {
        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, Quaternionf> codec() {
            return ByteBufCodecs.QUATERNIONF.map(Quaternionf::new, q -> q);
        }

        @Override
        public Quaternionf copy(Quaternionf value) {
            return new Quaternionf(value);
        }
    };

    /** A compound tag, copied on set. */
    public static final EntityDataSerializer<CompoundTag> COMPOUND = new EntityDataSerializer<>() {
        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, CompoundTag> codec() {
            return ByteBufCodecs.COMPOUND_TAG;
        }

        @Override
        public CompoundTag copy(CompoundTag value) {
            return value.copy();
        }
    };

    static {
        FabricTrackedDataRegistry.register(Ids.of("modularity", "compound_tag"), COMPOUND);
        FabricTrackedDataRegistry.register(Ids.of("modularity", "quaternion"), QUATERNION);
    }

    private SyncedData() {
    }

    /** Registers the serializers; called once during mod initialization, on both sides. */
    public static void init() {
    }
}
