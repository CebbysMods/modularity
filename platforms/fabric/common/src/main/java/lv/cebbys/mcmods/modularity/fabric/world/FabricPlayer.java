package lv.cebbys.mcmods.modularity.fabric.world;

import lv.cebbys.mcmods.modularity.api.block.BlockStateRef;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.api.transfer.FluidTank;
import lv.cebbys.mcmods.modularity.api.world.Player;
import lv.cebbys.mcmods.modularity.fabric.Wrap;
import lv.cebbys.mcmods.modularity.fabric.compat.Messages;
import lv.cebbys.mcmods.modularity.fabric.transfer.FabricFluidTank;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

public final class FabricPlayer extends NativeEntityRef implements Player {
    private final net.minecraft.world.entity.player.Player player;

    public FabricPlayer(net.minecraft.world.entity.player.Player player) {
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
        if (player.level().isClientSide() || !(tank instanceof FabricFluidTank fabricTank)) {
            return 0;
        }
        var context = ContainerItemContext.forPlayerInteraction(player, InteractionHand.MAIN_HAND);
        var source = context.find(FluidStorage.ITEM);
        if (source == null) {
            return 0;
        }
        long droplets = StorageUtil.move(source, fabricTank.storage(), variant -> true, Long.MAX_VALUE, null);
        return droplets * 1000 / FluidConstants.BUCKET;
    }

    @Override
    public void displayMessage(String message, boolean actionBar) {
        Messages.show(player, Component.literal(message), actionBar);
    }
}
