package lv.cebbys.mcmods.modularity.platform;

import lv.cebbys.mcmods.modularity.api.block.BlockRef;
import lv.cebbys.mcmods.modularity.api.block.BlockStateRef;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.api.world.Direction;
import lv.cebbys.mcmods.modularity.api.world.Pos;
import lv.cebbys.mcmods.modularity.api.world.Vec3;
import lv.cebbys.mcmods.modularity.platform.compat.Ids;
import lv.cebbys.mcmods.modularity.platform.entity.ModularEntity;
import lv.cebbys.mcmods.modularity.platform.world.McBlockRef;
import lv.cebbys.mcmods.modularity.platform.world.McBlockStateRef;
import lv.cebbys.mcmods.modularity.platform.world.McItemStack;
import lv.cebbys.mcmods.modularity.platform.world.McLevel;
import lv.cebbys.mcmods.modularity.platform.world.McPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Conversions between native Minecraft objects and portable Modularity handles. */
public final class Wrap {
    private Wrap() {
    }

    public static BlockPos pos(Pos pos) {
        return new BlockPos(pos.x(), pos.y(), pos.z());
    }

    public static Pos pos(BlockPos pos) {
        return new Pos(pos.getX(), pos.getY(), pos.getZ());
    }

    public static Vec3 vec(net.minecraft.world.phys.Vec3 vec) {
        return new Vec3(vec.x, vec.y, vec.z);
    }

    public static Direction direction(net.minecraft.core.Direction direction) {
        return Direction.valueOf(direction.name());
    }

    public static net.minecraft.core.Direction direction(Direction direction) {
        return net.minecraft.core.Direction.valueOf(direction.name());
    }

    public static BlockRef block(Block block) {
        return new McBlockRef(block);
    }

    public static BlockStateRef state(BlockState state) {
        return new McBlockStateRef(state);
    }

    public static BlockState state(BlockStateRef state) {
        return state.unwrap(BlockState.class);
    }

    public static Block block(BlockRef block) {
        return block.unwrap(Block.class);
    }

    /** The registered block with that id; call it only after the block registry is open. */
    public static Block block(Id id) {
        return BuiltInRegistries.BLOCK.get(Ids.rl(id));
    }

    /** The registered entity type with that id; call it only after the entity registry is open. */
    @SuppressWarnings("unchecked")
    public static EntityType<ModularEntity> entityType(Id id) {
        return (EntityType<ModularEntity>) BuiltInRegistries.ENTITY_TYPE.get(Ids.rl(id));
    }

    public static ItemStackRef stack(ItemStack stack) {
        return new McItemStack(stack);
    }

    public static ItemStack stack(ItemStackRef stack) {
        return stack.unwrap(ItemStack.class);
    }

    public static McLevel level(Level level) {
        return new McLevel(level);
    }

    public static McPlayer player(net.minecraft.world.entity.player.Player player) {
        return new McPlayer(player);
    }

    public static lv.cebbys.mcmods.modularity.api.entity.EntityRef entity(net.minecraft.world.entity.Entity entity) {
        return entity instanceof net.minecraft.world.entity.player.Player player
                ? new McPlayer(player) : new lv.cebbys.mcmods.modularity.platform.world.NativeEntityRef(entity);
    }

    public static net.minecraft.world.phys.Vec3 vec(Vec3 vec) {
        return new net.minecraft.world.phys.Vec3(vec.x(), vec.y(), vec.z());
    }
}
