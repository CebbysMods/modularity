package lv.cebbys.mcmods.modularity.loom.compat;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Registering play payload types. */
public final class Payloads {
    private Payloads() {
    }

    /** Registers {@code type} for both directions of the play phase. */
    public static <T extends CustomPacketPayload> void registerPlay(CustomPacketPayload.Type<T> type,
                                                                   StreamCodec<FriendlyByteBuf, T> codec) {
        PayloadTypeRegistry.playC2S().register(type, codec);
        PayloadTypeRegistry.playS2C().register(type, codec);
    }
}
