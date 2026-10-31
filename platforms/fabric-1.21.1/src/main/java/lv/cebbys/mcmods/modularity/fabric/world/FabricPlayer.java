package lv.cebbys.mcmods.modularity.fabric.world;

import lv.cebbys.mcmods.modularity.api.block.BlockStateRef;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.api.world.Player;
import lv.cebbys.mcmods.modularity.fabric.Wrap;

public record FabricPlayer(net.minecraft.world.entity.player.Player player) implements Player {
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
    public ItemStackRef mainHandItem() {
        return Wrap.stack(player.getMainHandItem());
    }

    @Override
    public Object handle() {
        return player;
    }
}
