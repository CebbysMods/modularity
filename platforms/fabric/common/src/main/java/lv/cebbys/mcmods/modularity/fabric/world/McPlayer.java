package lv.cebbys.mcmods.modularity.fabric.world;

import lv.cebbys.mcmods.modularity.api.block.BlockStateRef;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.api.transfer.FluidTank;
import lv.cebbys.mcmods.modularity.api.world.Player;
import lv.cebbys.mcmods.modularity.fabric.Wrap;
import lv.cebbys.mcmods.modularity.fabric.compat.Messages;
import lv.cebbys.mcmods.modularity.fabric.loader.Loaders;
import lv.cebbys.mcmods.modularity.fabric.transfer.McFluidTank;
import net.minecraft.network.chat.Component;

public final class McPlayer extends NativeEntityRef implements Player {
    private final net.minecraft.world.entity.player.Player player;

    public McPlayer(net.minecraft.world.entity.player.Player player) {
        super(player);
        this.player = player;
    }

    @Override
    public String name() {
        return player.getName().getString();
    }

    @Override
    public float destroySpeed(BlockStateRef state) {
        return player.getDestroySpeed(Wrap.state(state));
    }

    @Override
    public boolean hasCorrectToolForDrops(BlockStateRef state) {
        return player.hasCorrectToolForDrops(Wrap.state(state));
    }

    @Override
    public boolean isCreative() {
        return player.isCreative();
    }

    @Override
    public boolean isSpectator() {
        return player.isSpectator();
    }

    @Override
    public boolean isShiftKeyDown() {
        return player.isShiftKeyDown();
    }

    @Override
    public ItemStackRef mainHandItem() {
        return Wrap.stack(player.getMainHandItem());
    }

    @Override
    public long fillFromHeldItem(FluidTank tank) {
        if (player.level().isClientSide() || !(tank instanceof McFluidTank mcTank)) {
            return 0;
        }
        return Loaders.storage().fillFromHeldItem(player, mcTank);
    }

    @Override
    public void displayMessage(String message, boolean actionBar) {
        Messages.show(player, Component.literal(message), actionBar);
    }
}
