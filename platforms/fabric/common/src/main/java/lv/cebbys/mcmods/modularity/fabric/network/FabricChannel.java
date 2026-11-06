package lv.cebbys.mcmods.modularity.fabric.network;

import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.data.DataCodec;
import lv.cebbys.mcmods.modularity.api.network.Channel;
import lv.cebbys.mcmods.modularity.api.world.Player;
import lv.cebbys.mcmods.modularity.fabric.Wrap;
import lv.cebbys.mcmods.modularity.fabric.block.NbtData;
import lv.cebbys.mcmods.modularity.fabric.compat.Ids;
import lv.cebbys.mcmods.modularity.fabric.compat.Payloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;

/** A typed channel: one custom payload type per direction, the message body is NBT via the codec. */
public final class FabricChannel<T> implements Channel<T> {
    private final Id id;
    private final DataCodec<T> codec;
    private final CustomPacketPayload.Type<Payload> type;

    /** Payload carrying the encoded message. */
    public record Payload(CustomPacketPayload.Type<Payload> payloadType, CompoundTag body) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return payloadType;
        }
    }

    public FabricChannel(Id id, DataCodec<T> codec) {
        this.id = id;
        this.codec = codec;
        this.type = new CustomPacketPayload.Type<>(Ids.rl(id));
        StreamCodec<FriendlyByteBuf, Payload> streamCodec = StreamCodec.of(
                (buf, payload) -> buf.writeNbt(payload.body()),
                buf -> new Payload(type, buf.readNbt()));
        Payloads.registerPlay(type, streamCodec);
    }

    public CustomPacketPayload.Type<Payload> type() {
        return type;
    }

    public Payload encode(T message) {
        return new Payload(type, NbtData.encode(codec, message));
    }

    public T decode(Payload payload) {
        return NbtData.decode(codec, payload.body());
    }

    @Override
    public Id id() {
        return id;
    }

    @Override
    public void sendToServer(T message) {
        ClientSender.send(encode(message));
    }

    @Override
    public void sendTo(Player player, T message) {
        ServerPlayNetworking.send(player.unwrap(ServerPlayer.class), encode(message));
    }

    @Override
    public void onServer(BiConsumer<Player, T> handler) {
        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) ->
                handler.accept(Wrap.player(context.player()), decode(payload)));
    }

    /** Indirection so dedicated servers never load client networking classes. */
    public static final class ClientSender {
        private static volatile java.util.function.Consumer<CustomPacketPayload> sender = payload -> {
            throw new IllegalStateException("sendToServer is only available on the client");
        };

        private ClientSender() {
        }

        public static void install(java.util.function.Consumer<CustomPacketPayload> clientSender) {
            sender = clientSender;
        }

        static void send(CustomPacketPayload payload) {
            sender.accept(payload);
        }
    }
}
