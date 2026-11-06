package lv.cebbys.mcmods.modularity.fabric.world;

import lv.cebbys.mcmods.modularity.api.block.BlockRef;
import lv.cebbys.mcmods.modularity.api.block.BlockStateRef;
import lv.cebbys.mcmods.modularity.api.block.BlockType;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.fabric.Wrap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;

public record FabricBlockRef(Block block) implements BlockRef {
    @Override
    public Id id() {
        return Wrap.id(BuiltInRegistries.BLOCK.getKey(block));
    }

    @Override
    public BlockStateRef defaultState() {
        return Wrap.state(block.defaultBlockState());
    }

    @Override
    public boolean is(BlockType type) {
        return switch (type) {
            case SLAB -> block instanceof SlabBlock;
            case STAIRS -> block instanceof StairBlock;
            case WALL -> block instanceof WallBlock;
            case FENCE -> block instanceof FenceBlock;
            case FENCE_GATE -> block instanceof FenceGateBlock;
            case DOOR -> block instanceof DoorBlock;
            case TRAPDOOR -> block instanceof TrapDoorBlock;
            case BUTTON -> block instanceof ButtonBlock;
            case PRESSURE_PLATE -> block instanceof BasePressurePlateBlock;
            case LEAVES -> block instanceof LeavesBlock;
            case FALLING -> block instanceof FallingBlock;
        };
    }

    @Override
    public ItemStackRef asStack(int count) {
        return Wrap.stack(new ItemStack(block.asItem(), count));
    }

    @Override
    public Object handle() {
        return block;
    }
}
