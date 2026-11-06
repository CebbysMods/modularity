package lv.cebbys.mcmods.modularity.fabric.compat;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricTrackedDataRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceLocation;

/**
 * Serializers for synced entity data. 1.21.9: vanilla has no compound serializer any more; ours is registered with
 * Fabric under {@code modularity:compound_tag}, so client and server agree on its id.
 */
public final class SyncedData {
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
        FabricTrackedDataRegistry.register(ResourceLocation.fromNamespaceAndPath("modularity", "compound_tag"), COMPOUND);
    }

    private SyncedData() {
    }

    /** Registers the serializers; called once during mod initialization, on both sides. */
    public static void init() {
    }
}
