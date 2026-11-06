package lv.cebbys.mcmods.modularity.fabric.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Messages to one player. 26.1: separate methods for chat (system) and overlay (action bar) messages. */
public final class Messages {
    private Messages() {
    }

    /** Shows {@code message} in the chat, or above the hotbar when {@code actionBar}. */
    public static void show(Player player, Component message, boolean actionBar) {
        if (actionBar) {
            player.sendOverlayMessage(message);
        } else {
            player.sendSystemMessage(message);
        }
    }
}
