package lv.cebbys.mcmods.modularity.fabric.compat;

import lv.cebbys.mcmods.modularity.api.client.MovementInput;
import net.minecraft.client.player.LocalPlayer;

/** Client calls. Only loaded on the client. */
public final class ClientCompat {
    private ClientCompat() {
    }

    /** The movement keys the local player holds this tick. */
    public static MovementInput movement(LocalPlayer player) {
        var input = player.input;
        return new MovementInput(input.up, input.down, input.left, input.right, input.jumping, input.shiftKeyDown);
    }
}
