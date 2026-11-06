package lv.cebbys.mcmods.modularity.fabric.client;

import lv.cebbys.mcmods.modularity.fabric.compat.ClientCompat;
import lv.cebbys.mcmods.modularity.api.client.ClientState;
import lv.cebbys.mcmods.modularity.api.client.MovementInput;
import lv.cebbys.mcmods.modularity.api.world.Player;
import lv.cebbys.mcmods.modularity.fabric.Wrap;
import net.minecraft.client.Minecraft;

import java.util.Optional;

final class FabricClientState implements ClientState {
    static final FabricClientState INSTANCE = new FabricClientState();

    @Override
    public Optional<Player> player() {
        var player = Minecraft.getInstance().player;
        return player == null ? Optional.empty() : Optional.of(Wrap.player(player));
    }

    @Override
    public MovementInput movement() {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return MovementInput.NONE;
        }
        return ClientCompat.movement(player);
    }

    @Override
    public boolean isPaused() {
        return Minecraft.getInstance().isPaused();
    }
}
