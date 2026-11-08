package lv.cebbys.mcmods.modularity.api.network;

import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.world.Player;

import java.util.function.BiConsumer;

/**
 * Typed messages between client and server, serialized with a {@code DataCodec}. Created with
 * {@code mod.channel("controls", Controls.CODEC)}; handlers run on the game thread.
 *
 * <ul>
 *     <li>client → server: {@link #sendToServer}, handled by {@link #onServer}</li>
 *     <li>server → client: {@link #sendTo}, handled by {@code ClientContext#onMessage}</li>
 * </ul>
 */
public interface Channel<T> {
    Id id();

    /** Client side. */
    void sendToServer(T message);

    /** Server side. */
    void sendTo(Player player, T message);

    /** Registers the server handler; {@code Player} is the sender. */
    void onServer(BiConsumer<Player, T> handler);
}
