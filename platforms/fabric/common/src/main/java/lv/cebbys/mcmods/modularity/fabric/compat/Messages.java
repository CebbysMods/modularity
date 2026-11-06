package lv.cebbys.mcmods.modularity.fabric.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Messages to one player. */
public final class Messages {
    private Messages() {
    }

    /** Shows {@code message} in the chat, or above the hotbar when {@code actionBar}. */
    public static void show(Player player, Component message, boolean actionBar) {
        player.displayClientMessage(message, actionBar);
    }
}
