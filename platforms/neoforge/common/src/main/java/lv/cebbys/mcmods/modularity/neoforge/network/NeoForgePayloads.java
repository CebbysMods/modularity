package lv.cebbys.mcmods.modularity.neoforge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The payloads Modularity registers on NeoForge. Every payload is bidirectional; the handler looks up the listener for
 * the side that received it when the payload arrives, so a listener may be added before or after the registration
 * event. The handlers run on the game thread through {@code enqueueWork}.
 */
public final class NeoForgePayloads {
    private static final Map<CustomPacketPayload.Type<?>, StreamCodec<FriendlyByteBuf, ? extends CustomPacketPayload>>
            CODECS = new LinkedHashMap<>();
    private static final Map<CustomPacketPayload.Type<?>, BiConsumer<ServerPlayer, ?>> SERVER = new LinkedHashMap<>();
    private static final Map<CustomPacketPayload.Type<?>, Consumer<?>> CLIENT = new LinkedHashMap<>();

    private NeoForgePayloads() {
    }

    /** Queues a payload; the registration event sends every queued payload to the network handler. */
    public static <P extends CustomPacketPayload> void register(CustomPacketPayload.Type<P> type,
                                                               StreamCodec<FriendlyByteBuf, P> codec) {
        CODECS.put(type, codec);
    }

    public static <P extends CustomPacketPayload> void onServer(CustomPacketPayload.Type<P> type,
                                                                BiConsumer<ServerPlayer, P> handler) {
        SERVER.put(type, handler);
    }

    public static <P extends CustomPacketPayload> void onClient(CustomPacketPayload.Type<P> type, Consumer<P> handler) {
        CLIENT.put(type, handler);
    }

    /** Adds the registration listener to the mod bus. */
    public static void install(IEventBus modBus) {
        modBus.addListener((RegisterPayloadHandlersEvent event) -> {
            PayloadRegistrar registrar = event.registrar("1");
            CODECS.forEach((type, codec) -> installRaw(registrar, type, codec));
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void installRaw(PayloadRegistrar registrar, CustomPacketPayload.Type<?> type,
                                   StreamCodec<FriendlyByteBuf, ? extends CustomPacketPayload> codec) {
        installOne(registrar, (CustomPacketPayload.Type) type, (StreamCodec) codec);
    }

    @SuppressWarnings("unchecked")
    private static <P extends CustomPacketPayload> void installOne(PayloadRegistrar registrar,
                                                                   CustomPacketPayload.Type<P> type,
                                                                   StreamCodec<FriendlyByteBuf, P> codec) {
        StreamCodec<? super RegistryFriendlyByteBuf, P> netCodec = (StreamCodec<? super RegistryFriendlyByteBuf, P>) codec;
        registrar.playBidirectional(type, netCodec, (payload, context) -> context.enqueueWork(() -> {
            if (context.flow() == PacketFlow.SERVERBOUND) {
                BiConsumer<ServerPlayer, P> handler = (BiConsumer<ServerPlayer, P>) SERVER.get(type);
                if (handler != null && context.player() instanceof ServerPlayer player) {
                    handler.accept(player, payload);
                }
            } else {
                Consumer<P> handler = (Consumer<P>) CLIENT.get(type);
                if (handler != null) {
                    handler.accept(payload);
                }
            }
        }));
    }
}
