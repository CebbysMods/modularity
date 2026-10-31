package lv.cebbys.mcmods.modularity.api.client;

import lv.cebbys.mcmods.modularity.api.world.Player;

import java.util.Optional;

/** The local client. */
public interface ClientState {
    /** The local player while in a world. */
    Optional<Player> player();

    /** Movement keys of the local player (respects the player's key bindings). */
    MovementInput movement();

    boolean isPaused();
}
