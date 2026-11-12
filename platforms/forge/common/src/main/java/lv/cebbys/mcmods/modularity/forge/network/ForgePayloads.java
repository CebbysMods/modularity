package lv.cebbys.mcmods.modularity.forge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.payload.PayloadConnection;
import net.minecraftforge.network.payload.PayloadFlow;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The payloads Modularity registers on Forge. The channel is built at common setup, because a Forge channel is
 * fixed once it is built and the common stage is what registers the codecs. Every payload is bidirectional; the
 * handler looks up the listener for the side that received it when the payload arrives, so a listener may be added
 * before or after the channel is built. The handlers run on the game thread through {@code enqueueWork}.
 */
public final class ForgePayloads {
    private static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath("modularity", "main");
    private static final Map<CustomPacketPayload.Type<?>, StreamCodec<FriendlyByteBuf, ? extends CustomPacketPayload>>
            CODECS = new LinkedHashMap<>();
    private static final Map<CustomPacketPayload.Type<?>, BiConsumer<ServerPlayer, ?>> SERVER = new LinkedHashMap<>();
    private static final Map<CustomPacketPayload.Type<?>, Consumer<?>> CLIENT = new LinkedHashMap<>();

    private static PayloadConnection<CustomPacketPayload> connection;
    private static Channel<CustomPacketPayload> channel;

    private ForgePayloads() {
    }

    /** Queues a payload; the channel sends every queued payload to the network handler when it is built. */
    public static <P extends CustomPacketPayload> void register(CustomPacketPayload.Type<P> type,
                                                               StreamCodec<FriendlyByteBuf, P> codec) {
        if (channel != null) {
            throw new IllegalStateException(
                    "Modularity: payload " + type.id() + " registered after the Forge channel was built");
        }
        CODECS.put(type, codec);
    }

    public static <P extends CustomPacketPayload> void onServer(CustomPacketPayload.Type<P> type,
                                                                BiConsumer<ServerPlayer, P> handler) {
        SERVER.put(type, handler);
    }

    public static <P extends CustomPacketPayload> void onClient(CustomPacketPayload.Type<P> type, Consumer<P> handler) {
        CLIENT.put(type, handler);
    }

    /** Creates the channel and builds it at common setup, before any connection exists. */
    public static void install(IEventBus modBus) {
        connection = ChannelBuilder.named(CHANNEL).networkProtocolVersion(1).optional().payloadChannel();
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(ForgePayloads::build));
    }

    /** Sends {@code payload} to one player. */
    public static void sendTo(ServerPlayer player, CustomPacketPayload payload) {
        build();
        channel.send(payload, PacketDistributor.PLAYER.with(player));
    }

    /** Sends {@code payload} to the server. */
    public static void sendToServer(CustomPacketPayload payload) {
        build();
        channel.send(payload, PacketDistributor.SERVER.noArg());
    }

    /** Builds the channel once; a send before common setup builds it early. */
    private static synchronized void build() {
        if (channel != null) {
            return;
        }
        PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> flow = connection.play().bidirectional();
        for (Map.Entry<CustomPacketPayload.Type<?>, StreamCodec<FriendlyByteBuf, ? extends CustomPacketPayload>>
                entry : CODECS.entrySet()) {
            addRaw(flow, entry.getKey(), entry.getValue());
        }
        channel = flow.build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addRaw(PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> flow,
                               CustomPacketPayload.Type<?> type,
                               StreamCodec<FriendlyByteBuf, ? extends CustomPacketPayload> codec) {
        flow.addMain((CustomPacketPayload.Type) type, (StreamCodec) codec, ForgePayloads::dispatch);
    }

    @SuppressWarnings("unchecked")
    private static <P extends CustomPacketPayload> void dispatch(P payload, CustomPayloadEvent.Context context) {
        CustomPacketPayload.Type<?> type = payload.type();
        if (context.isClientSide()) {
            Consumer<P> handler = (Consumer<P>) CLIENT.get(type);
            if (handler != null) {
                handler.accept(payload);
            }
            return;
        }
        BiConsumer<ServerPlayer, P> handler = (BiConsumer<ServerPlayer, P>) SERVER.get(type);
        ServerPlayer sender = context.getSender();
        if (handler != null && sender != null) {
            handler.accept(sender, payload);
        }
    }
}
