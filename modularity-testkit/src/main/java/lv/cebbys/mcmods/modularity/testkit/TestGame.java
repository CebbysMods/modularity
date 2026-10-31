package lv.cebbys.mcmods.modularity.testkit;

import lv.cebbys.mcmods.modularity.api.ModContext;
import lv.cebbys.mcmods.modularity.api.Modularity;
import lv.cebbys.mcmods.modularity.api.ModularMod;
import lv.cebbys.mcmods.modularity.api.Platform;
import lv.cebbys.mcmods.modularity.api.block.BlockBehaviour;
import lv.cebbys.mcmods.modularity.api.block.BlockContext;
import lv.cebbys.mcmods.modularity.api.block.BlockDefinition;
import lv.cebbys.mcmods.modularity.api.block.BlockHandle;
import lv.cebbys.mcmods.modularity.api.block.BlockInterceptor;
import lv.cebbys.mcmods.modularity.api.block.BlockRef;
import lv.cebbys.mcmods.modularity.api.block.BlockSelector;
import lv.cebbys.mcmods.modularity.api.block.BlockStateRef;
import lv.cebbys.mcmods.modularity.api.block.BreakContext;
import lv.cebbys.mcmods.modularity.api.block.DropContext;
import lv.cebbys.mcmods.modularity.api.block.PlacementContext;
import lv.cebbys.mcmods.modularity.api.block.PlacementResult;
import lv.cebbys.mcmods.modularity.api.block.property.SlabType;
import lv.cebbys.mcmods.modularity.api.block.property.VanillaProperties;
import lv.cebbys.mcmods.modularity.api.client.ClientContext;
import lv.cebbys.mcmods.modularity.api.client.DynamicModel;
import lv.cebbys.mcmods.modularity.api.client.HitParticles;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.core.Side;
import lv.cebbys.mcmods.modularity.api.data.BlockData;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.api.world.BlockView;
import lv.cebbys.mcmods.modularity.api.world.Direction;
import lv.cebbys.mcmods.modularity.api.world.Level;
import lv.cebbys.mcmods.modularity.api.world.Player;
import lv.cebbys.mcmods.modularity.api.world.Pos;
import lv.cebbys.mcmods.modularity.api.world.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A tiny, deterministic stand-in for Minecraft. It implements the Modularity runtime and simulates
 * the vanilla code paths the API hooks into (block placement, mining, breaking, drops, rendering),
 * so mod logic written against the API can be unit tested without starting the game.
 *
 * <pre>{@code
 * TestGame game = TestGame.create();
 * TestBlock oak = game.slab("minecraft:oak_slab");
 * game.load("refabslab", new RefabslabMod());
 * TestLevel level = game.level();
 * }</pre>
 */
public final class TestGame implements Modularity.Runtime {
    private final TestPlatform platform = new TestPlatform();
    private final Map<Id, TestBlock> blocks = new LinkedHashMap<>();
    private final List<Interceptor> interceptors = new ArrayList<>();
    private final Map<Id, DynamicModel> models = new HashMap<>();
    private final Map<Id, HitParticles> hitParticles = new HashMap<>();
    private final TestBlock air;

    private TestGame() {
        air = register(TestBlock.simple(Id.minecraft("air")).hardness(0.0F).noItem());
    }

    /** Creates a game and installs it as the active Modularity runtime. */
    public static TestGame create() {
        TestGame game = new TestGame();
        Modularity.install(game);
        return game;
    }

    private record Interceptor(BlockSelector selector, BlockInterceptor interceptor) {
    }

    // ---------------------------------------------------------------- setup

    public TestPlatform testPlatform() {
        return platform;
    }

    public TestBlock air() {
        return air;
    }

    public TestBlock slab(String id) {
        return register(TestBlock.slab(Id.parse(id)));
    }

    public TestBlock block(String id) {
        return register(TestBlock.simple(Id.parse(id)));
    }

    private TestBlock register(TestBlock block) {
        if (blocks.putIfAbsent(block.id(), block) != null) {
            throw new IllegalStateException("Duplicate block " + block.id());
        }
        return block;
    }

    /** Runs the mod's common init and, on the client side, its client init. */
    public TestGame load(String modId, ModularMod mod) {
        mod.init(new TestModContext(modId));
        if (platform.side() == Side.CLIENT) {
            mod.initClient(new TestClientContext(modId));
        }
        return this;
    }

    public TestLevel level() {
        return new TestLevel(this, false);
    }

    public TestLevel clientLevel() {
        return new TestLevel(this, true);
    }

    // ---------------------------------------------------------------- runtime

    @Override
    public Platform platform() {
        return platform;
    }

    @Override
    public Optional<BlockRef> block(Id id) {
        return Optional.ofNullable(blocks.get(id));
    }

    @Override
    public ItemStackRef emptyStack() {
        return TestItemStack.EMPTY;
    }

    // ---------------------------------------------------------------- simulated game actions

    /** Outcome of {@link #use}. */
    public record PlaceOutcome(boolean placed, Pos pos, BlockStateRef state) {
    }

    /**
     * Simulates a player right-clicking {@code face} of the block at {@code clicked} at world
     * position {@code hit} with {@code stack} (vanilla {@code BlockItem#place}).
     */
    public PlaceOutcome use(TestLevel level, Pos clicked, Direction face, Vec3 hit, ItemStackRef stack, Player player) {
        Optional<BlockRef> placing = stack.block();
        if (placing.isEmpty()) {
            return new PlaceOutcome(false, clicked, level.blockState(clicked));
        }
        Ctx atClicked = new Ctx(level, clicked, face, hit, stack, player);
        Pos target = canBeReplaced(level.blockState(clicked), atClicked) ? clicked : clicked.offset(face);
        Ctx ctx = new Ctx(level, target, face, hit, stack, player);
        if (!target.equals(clicked) && !canBeReplaced(level.blockState(target), ctx)) {
            return new PlaceOutcome(false, target, level.blockState(target));
        }
        PlacementResult result = placementState((TestBlock) placing.get(), ctx);
        if (result == null || !level.isUnobstructed(result.state(), target)) {
            return new PlaceOutcome(false, target, level.blockState(target));
        }
        level.set(target, result.state());
        level.setRawData(target, result.data());
        return new PlaceOutcome(true, target, result.state());
    }

    /** Vanilla {@code BlockState#canBeReplaced(BlockPlaceContext)} including interceptors. */
    public boolean canBeReplaced(BlockStateRef existing, PlacementContext ctx) {
        for (Interceptor entry : interceptors) {
            if (entry.selector().test(existing.block())) {
                Optional<Boolean> answer = entry.interceptor().canBeReplaced(existing, ctx);
                if (answer.isPresent()) {
                    return answer.get();
                }
            }
        }
        if (existing.isAir()) {
            return true;
        }
        if (existing.has(VanillaProperties.SLAB_TYPE)) {
            SlabType type = existing.get(VanillaProperties.SLAB_TYPE);
            boolean sameSlab = ctx.stack().block().map(b -> b.equals(existing.block())).orElse(false);
            return sameSlab && type != SlabType.DOUBLE
                    && (type == SlabType.BOTTOM && ctx.clickedFace() == Direction.UP
                    || type == SlabType.TOP && ctx.clickedFace() == Direction.DOWN);
        }
        return false;
    }

    /** Builds a placement context, e.g. for falling blocks: empty stack, no player. */
    public PlacementContext placementContext(TestLevel level, Pos pos, Direction face, Vec3 hit, ItemStackRef stack, Player player) {
        return new Ctx(level, pos, face, hit, stack, player);
    }

    private PlacementResult placementState(TestBlock placing, Ctx ctx) {
        for (Interceptor entry : interceptors) {
            if (entry.selector().test(placing)) {
                Optional<PlacementResult> answer = entry.interceptor().placement(ctx);
                if (answer.isPresent()) {
                    return answer.get();
                }
            }
        }
        BlockStateRef existing = ctx.existingState();
        BlockStateRef state = placing.defaultState();
        if (state.has(VanillaProperties.SLAB_TYPE)) {
            if (existing.block().equals(placing)) {
                state = state.with(VanillaProperties.SLAB_TYPE, SlabType.DOUBLE);
            } else {
                boolean top = ctx.clickedFace() == Direction.DOWN
                        || ctx.clickedFace() != Direction.UP && ctx.clickLocation().y() - ctx.pos().y() > 0.5D;
                state = state.with(VanillaProperties.SLAB_TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
            }
        }
        return PlacementResult.of(state);
    }

    /** Vanilla {@code getDestroyProgress}. */
    public float destroyProgress(TestLevel level, Pos pos, Player player) {
        BlockStateRef state = level.blockState(pos);
        float custom = behaviour(state).destroyProgress(new BlockCtx(level, pos, state), player);
        if (!Float.isNaN(custom)) {
            return custom;
        }
        float hardness = state.destroySpeed(level, pos);
        if (hardness == -1.0F) {
            return 0.0F;
        }
        return player.destroySpeed(state) / hardness / (canHarvest(state, player) ? 30.0F : 100.0F);
    }

    /** A player breaks the block: break hook, drops (survival + correct tool), removal. */
    public void breakBlock(TestLevel level, Pos pos, Player player) {
        BlockStateRef state = level.blockState(pos);
        behaviour(state).onPlayerBreak(new BreakCtx(level, pos, state, player));
        Map<BlockData<?>, Object> data = level.snapshot(pos);
        level.set(pos, air.defaultState());
        if (!level.isClientSide() && !player.isCreative() && canHarvest(state, player)) {
            drops(level, pos, state, data, player).forEach(stack -> level.dropItem(pos, stack));
        }
    }

    /** The block is destroyed without a player (explosion). */
    public void explode(TestLevel level, Pos pos) {
        BlockStateRef state = level.blockState(pos);
        Map<BlockData<?>, Object> data = level.snapshot(pos);
        level.set(pos, air.defaultState());
        drops(level, pos, state, data, null).forEach(stack -> level.dropItem(pos, stack));
    }

    private List<ItemStackRef> drops(TestLevel level, Pos pos, BlockStateRef state, Map<BlockData<?>, Object> data, Player player) {
        return behaviour(state).drops(new DropCtx(level, pos, state, data, player));
    }

    /** What the {@link DynamicModel} of the block at {@code pos} renders. */
    public List<BlockStateRef> render(BlockView view, Pos pos) {
        BlockStateRef state = view.blockState(pos);
        DynamicModel model = models.get(state.block().id());
        if (model == null) {
            return List.of(state);
        }
        List<BlockStateRef> out = new ArrayList<>();
        model.emit(view, pos, state, out::add);
        return out;
    }

    public Optional<DynamicModel> model(Id block) {
        return Optional.ofNullable(models.get(block));
    }

    /** Texture source for the "being hit" particle spawned at {@code particle}. */
    public BlockStateRef hitParticleState(BlockView view, Pos pos, Direction face, Vec3 particle) {
        BlockStateRef state = view.blockState(pos);
        HitParticles handler = hitParticles.get(state.block().id());
        return handler == null ? state : handler.particleState(view, pos, state, face, particle);
    }

    private static boolean canHarvest(BlockStateRef state, Player player) {
        BlockDefinition definition = ((TestBlock) state.block()).definition();
        boolean requiresTool = definition != null && definition.requiresCorrectTool();
        return !requiresTool || player.hasCorrectToolForDrops(state);
    }

    private static BlockBehaviour behaviour(BlockStateRef state) {
        BlockDefinition definition = ((TestBlock) state.block()).definition();
        return definition == null ? BlockBehaviour.VANILLA : definition.behaviour();
    }

    // ---------------------------------------------------------------- contexts

    private final class TestModContext implements ModContext {
        private final String modId;

        private TestModContext(String modId) {
            this.modId = modId;
        }

        @Override
        public String modId() {
            return modId;
        }

        @Override
        public Platform platform() {
            return platform;
        }

        @Override
        public System.Logger logger() {
            return System.getLogger(modId);
        }

        @Override
        public BlockHandle block(String name, Consumer<BlockDefinition.Builder> spec) {
            BlockDefinition.Builder builder = BlockDefinition.builder();
            spec.accept(builder);
            BlockDefinition definition = builder.build();
            TestBlock block = register(new TestBlock(id(name), java.util.Set.of(), definition.properties(), definition));
            return new BlockHandle() {
                @Override
                public Id id() {
                    return block.id();
                }

                @Override
                public BlockDefinition definition() {
                    return definition;
                }

                @Override
                public BlockRef get() {
                    return block;
                }
            };
        }

        @Override
        public void intercept(BlockSelector selector, BlockInterceptor interceptor) {
            interceptors.add(new Interceptor(selector, interceptor));
        }
    }

    private final class TestClientContext implements ClientContext {
        private final String modId;

        private TestClientContext(String modId) {
            this.modId = modId;
        }

        @Override
        public String modId() {
            return modId;
        }

        @Override
        public Platform platform() {
            return platform;
        }

        @Override
        public void blockModel(BlockHandle block, DynamicModel model) {
            models.put(block.id(), model);
        }

        @Override
        public void hitParticles(BlockHandle block, HitParticles particles) {
            hitParticles.put(block.id(), particles);
        }
    }

    private record Ctx(TestLevel level, Pos pos, Direction clickedFace, Vec3 clickLocation, ItemStackRef stack,
                       Player playerOrNull) implements PlacementContext {
        @Override
        public Optional<Player> player() {
            return Optional.ofNullable(playerOrNull);
        }
    }

    private static class BlockCtx implements BlockContext {
        final TestLevel level;
        final Pos pos;
        final BlockStateRef state;

        BlockCtx(TestLevel level, Pos pos, BlockStateRef state) {
            this.level = level;
            this.pos = pos;
            this.state = state;
        }

        @Override
        public BlockView view() {
            return level;
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
            return level.data(pos, data);
        }
    }

    private static final class BreakCtx extends BlockCtx implements BreakContext {
        private final Player player;

        BreakCtx(TestLevel level, Pos pos, BlockStateRef state, Player player) {
            super(level, pos, state);
            this.player = player;
        }

        @Override
        public Level level() {
            return level;
        }

        @Override
        public Player player() {
            return player;
        }

        @Override
        public void breakEffects(BlockStateRef state) {
            level.playBreakEffects(state);
        }
    }

    private static final class DropCtx extends BlockCtx implements DropContext {
        private final Map<BlockData<?>, Object> data;
        private final Player player;

        DropCtx(TestLevel level, Pos pos, BlockStateRef state, Map<BlockData<?>, Object> data, Player player) {
            super(level, pos, state);
            this.data = data;
            this.player = player;
        }

        @Override
        public <T> Optional<T> data(BlockData<T> key) {
            Object value = data.get(key);
            return Optional.ofNullable(value).map(v -> {
                @SuppressWarnings("unchecked") T typed = (T) v;
                return typed;
            });
        }

        @Override
        public Level level() {
            return level;
        }

        @Override
        public Optional<Player> player() {
            return Optional.ofNullable(player);
        }

        @Override
        public ItemStackRef tool() {
            return player == null ? TestItemStack.EMPTY : player.mainHandItem();
        }

        @Override
        public List<ItemStackRef> vanillaDrops() {
            BlockDefinition definition = ((TestBlock) state.block()).definition();
            if (definition != null && !definition.hasLootTable()) {
                return List.of();
            }
            ItemStackRef stack = state.block().asStack(1);
            return stack.isEmpty() ? List.of() : List.of(stack);
        }
    }
}
