package lv.cebbys.mcmods.modularity.platform.block;

import lv.cebbys.mcmods.modularity.api.block.BlockContext;
import lv.cebbys.mcmods.modularity.api.block.BlockStateRef;
import lv.cebbys.mcmods.modularity.api.block.BreakContext;
import lv.cebbys.mcmods.modularity.api.block.DropContext;
import lv.cebbys.mcmods.modularity.api.block.PlacementContext;
import lv.cebbys.mcmods.modularity.api.data.BlockData;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.api.world.BlockView;
import lv.cebbys.mcmods.modularity.api.world.Direction;
import lv.cebbys.mcmods.modularity.api.world.Level;
import lv.cebbys.mcmods.modularity.api.world.Player;
import lv.cebbys.mcmods.modularity.api.world.Pos;
import lv.cebbys.mcmods.modularity.api.world.Vec3;
import lv.cebbys.mcmods.modularity.platform.Wrap;
import lv.cebbys.mcmods.modularity.platform.compat.GameCompat;
import lv.cebbys.mcmods.modularity.platform.world.NativeBlockView;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/** Portable context objects backed by native ones. */
public final class Contexts {
    private Contexts() {
    }

    public static PlacementContext placement(BlockPlaceContext ctx) {
        return new Placement(ctx);
    }

    public static BlockContext block(BlockGetter getter, BlockPos pos, BlockState state) {
        return new BlockBase(new NativeBlockView(getter), Wrap.pos(pos), Wrap.state(state));
    }

    public static BreakContext breaking(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
                                        net.minecraft.world.entity.player.Player player) {
        return new Break(Wrap.level(level), Wrap.pos(pos), Wrap.state(state), player);
    }

    public static DropContext drops(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
                                    ModularBlockEntity entity, net.minecraft.world.entity.player.Player player,
                                    ItemStack tool, Supplier<List<ItemStack>> vanilla) {
        return new Drops(Wrap.level(level), Wrap.pos(pos), Wrap.state(state), entity, player, tool, vanilla);
    }

    private record Placement(BlockPlaceContext ctx) implements PlacementContext {
        @Override
        public Level level() {
            return Wrap.level(ctx.getLevel());
        }

        @Override
        public Pos pos() {
            return Wrap.pos(ctx.getClickedPos());
        }

        @Override
        public Direction clickedFace() {
            return Wrap.direction(ctx.getClickedFace());
        }

        @Override
        public Vec3 clickLocation() {
            return Wrap.vec(ctx.getClickLocation());
        }

        @Override
        public ItemStackRef stack() {
            return Wrap.stack(ctx.getItemInHand());
        }

        @Override
        public Optional<Player> player() {
            return Optional.ofNullable(ctx.getPlayer()).map(Wrap::player);
        }
    }

    private static class BlockBase implements BlockContext {
        private final BlockView view;
        private final Pos pos;
        private final BlockStateRef state;

        BlockBase(BlockView view, Pos pos, BlockStateRef state) {
            this.view = view;
            this.pos = pos;
            this.state = state;
        }

        @Override
        public BlockView view() {
            return view;
        }

        @Override
        public Pos pos() {
            return pos;
        }

        @Override
        public BlockStateRef state() {
            return state;
        }

        @Override
        public <T> Optional<T> data(BlockData<T> data) {
            return view.data(pos, data);
        }
    }

    private static final class Break extends BlockBase implements BreakContext {
        private final Level level;
        private final net.minecraft.world.entity.player.Player player;

        Break(Level level, Pos pos, BlockStateRef state, net.minecraft.world.entity.player.Player player) {
            super(level, pos, state);
            this.level = level;
            this.player = player;
        }

        @Override
        public Level level() {
            return level;
        }

        @Override
        public Player player() {
            return Wrap.player(player);
        }

        @Override
        public void breakEffects(BlockStateRef state) {
            var nativeLevel = level.unwrap(net.minecraft.world.level.Level.class);
            var nativeState = Wrap.state(state);
            // Same as Block#spawnDestroyParticles: break particles and sound of that state.
            nativeLevel.levelEvent(player, 2001, Wrap.pos(pos()), Block.getId(nativeState));
            if (nativeState.is(BlockTags.GUARDED_BY_PIGLINS)) {
                GameCompat.angerNearbyPiglins(nativeLevel, player);
            }
        }
    }

    private static final class Drops extends BlockBase implements DropContext {
        private final Level level;
        private final ModularBlockEntity entity;
        private final net.minecraft.world.entity.player.Player player;
        private final ItemStack tool;
        private final Supplier<List<ItemStack>> vanilla;

        Drops(Level level, Pos pos, BlockStateRef state, ModularBlockEntity entity,
              net.minecraft.world.entity.player.Player player, ItemStack tool, Supplier<List<ItemStack>> vanilla) {
            super(level, pos, state);
            this.level = level;
            this.entity = entity;
            this.player = player;
            this.tool = tool;
            this.vanilla = vanilla;
        }

        @Override
        public <T> Optional<T> data(BlockData<T> data) {
            return entity == null ? Optional.empty() : entity.get(data);
        }

        @Override
        public Level level() {
            return level;
        }

        @Override
        public Optional<Player> player() {
            return Optional.ofNullable(player).map(Wrap::player);
        }

        @Override
        public ItemStackRef tool() {
            return Wrap.stack(tool == null ? ItemStack.EMPTY : tool);
        }

        @Override
        public List<ItemStackRef> vanillaDrops() {
            return vanilla.get().stream().map(Wrap::stack).toList();
        }
    }
}
