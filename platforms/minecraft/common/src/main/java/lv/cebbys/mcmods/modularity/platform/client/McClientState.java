package lv.cebbys.mcmods.modularity.platform.client;

import lv.cebbys.mcmods.modularity.api.client.ClientState;
import lv.cebbys.mcmods.modularity.api.client.MovementInput;
import lv.cebbys.mcmods.modularity.api.world.Player;
import lv.cebbys.mcmods.modularity.platform.Wrap;
import lv.cebbys.mcmods.modularity.platform.compat.ClientCompat;
import net.minecraft.client.Minecraft;

import java.util.Optional;

final class McClientState implements ClientState {
    static final McClientState INSTANCE = new McClientState();

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
