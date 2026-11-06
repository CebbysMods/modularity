package lv.cebbys.mcmods.modularity.fabric.compat;

import lv.cebbys.mcmods.modularity.api.client.MovementInput;
import net.minecraft.client.player.LocalPlayer;

/** Client calls. Only loaded on the client. 1.21.2: the held keys are an {@code Input} record. */
public final class ClientCompat {
    private ClientCompat() {
    }

    /** The movement keys the local player holds this tick. */
    public static MovementInput movement(LocalPlayer player) {
        var keys = player.input.keyPresses;
        return new MovementInput(keys.forward(), keys.backward(), keys.left(), keys.right(), keys.jump(), keys.shift());
    }
}
