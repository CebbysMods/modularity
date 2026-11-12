package lv.cebbys.mcmods.modularity.testkit;

import lv.cebbys.mcmods.modularity.api.CommonContext;
import lv.cebbys.mcmods.modularity.api.Modularity;
import lv.cebbys.mcmods.modularity.api.ModularityModClient;
import lv.cebbys.mcmods.modularity.api.ModularityModCommon;
import lv.cebbys.mcmods.modularity.api.ModularityModServer;
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
import lv.cebbys.mcmods.modularity.api.server.ServerContext;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.api.world.BlockView;
import lv.cebbys.mcmods.modularity.api.world.Direction;
import lv.cebbys.mcmods.modularity.api.world.Level;
import lv.cebbys.mcmods.modularity.api.world.Player;
import lv.cebbys.mcmods.modularity.api.world.Pos;
import lv.cebbys.mcmods.modularity.api.world.Vec3;

import lv.cebbys.mcmods.modularity.api.client.CameraController;
import lv.cebbys.mcmods.modularity.api.client.ClientState;
import lv.cebbys.mcmods.modularity.api.client.EntityRenderer;
import lv.cebbys.mcmods.modularity.api.client.HudContext;
import lv.cebbys.mcmods.modularity.api.client.HudLayer;
import lv.cebbys.mcmods.modularity.api.client.KeyBinding;
import lv.cebbys.mcmods.modularity.api.client.LoopingSound;
import lv.cebbys.mcmods.modularity.api.client.ModelRef;
import lv.cebbys.mcmods.modularity.api.client.MovementInput;
import lv.cebbys.mcmods.modularity.api.client.RenderOutput;
import lv.cebbys.mcmods.modularity.api.client.model.ObjModel;
import lv.cebbys.mcmods.modularity.api.client.model.ObjParser;
import lv.cebbys.mcmods.modularity.api.core.InteractionResult;
import lv.cebbys.mcmods.modularity.api.data.DataCodec;
import lv.cebbys.mcmods.modularity.api.data.MapData;
import lv.cebbys.mcmods.modularity.api.entity.EntityContext;
import lv.cebbys.mcmods.modularity.api.entity.EntityDefinition;
import lv.cebbys.mcmods.modularity.api.entity.EntityHandle;
import lv.cebbys.mcmods.modularity.api.entity.EntityRef;
import lv.cebbys.mcmods.modularity.api.item.ItemDefinition;
import lv.cebbys.mcmods.modularity.api.item.ItemHandle;
import lv.cebbys.mcmods.modularity.api.item.ItemUseContext;
import lv.cebbys.mcmods.modularity.api.network.Channel;
import lv.cebbys.mcmods.modularity.api.sound.SoundHandle;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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
    private final Map<Id, EntityHandle> entityTypes = new LinkedHashMap<>();
    private final Map<Id, ItemHandle> items = new LinkedHashMap<>();
    private final Map<Id, SoundHandle> sounds = new LinkedHashMap<>();
    private final Map<Id, TestChannel<?>> channels = new LinkedHashMap<>();
    private final Map<Id, EntityRenderer> entityRenderers = new HashMap<>();
    private final Map<String, TestKeyBinding> keyBindings = new LinkedHashMap<>();
    private final List<java.util.function.Consumer<ClientState>> tickHandlers = new ArrayList<>();
    private final List<CameraController> cameraControllers = new ArrayList<>();
    private final List<HudLayer> hudLayers = new ArrayList<>();
    private final List<LoopingSound> playingSounds = new ArrayList<>();
    private TestPlayer localPlayer;
    private MovementInput movement = MovementInput.NONE;
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

    /**
     * Runs the mod's common stage, then its client stage (side {@code CLIENT}) or its server stage
     * (side {@code SERVER}, i.e. a dedicated server). A mod takes part in a stage only when it
     * implements that stage's interface.
     */
    public TestGame load(String modId, ModularityModCommon mod) {
        mod.initialize(new TestModContext(modId));
        if (platform.side() == Side.CLIENT) {
            if (mod instanceof ModularityModClient client) {
                client.initialize(new TestClientContext(modId));
            }
        } else if (mod instanceof ModularityModServer server) {
            server.initialize(new TestServerContext(modId));
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

    // ---------------------------------------------------------------- entities, items, client

    public Optional<EntityHandle> entityType(Id id) {
        return Optional.ofNullable(entityTypes.get(id));
    }

    public Optional<ItemHandle> item(Id id) {
        return Optional.ofNullable(items.get(id));
    }

    public Optional<SoundHandle> sound(Id id) {
        return Optional.ofNullable(sounds.get(id));
    }

    /** One server tick: every Modularity entity in {@code level} ticks once. */
    public void tick(TestLevel level) {
        level.gameTime++;
        for (TestEntity entity : level.entities()) {
            if (entity.definition() != null && !entity.isRemoved()) {
                entity.definition().behaviour().tick(entityContext(entity));
            }
        }
    }

    public void tick(TestLevel level, int ticks) {
        for (int i = 0; i < ticks; i++) {
            tick(level);
        }
    }

    /** A player right-clicks {@code entity}. */
    public InteractionResult interact(TestEntity entity, TestPlayer player) {
        return entity.definition().behaviour().interact(entityContext(entity), player);
    }

    /** A player right-clicks the part {@code partName} of a multipart {@code entity}. */
    public InteractionResult interactPart(TestEntity entity, TestPlayer player, String partName) {
        return entity.definition().behaviour().interactPart(entityContext(entity), player, part(entity, partName));
    }

    /** {@code attacker} hits the part {@code partName} of a multipart {@code entity}. */
    public boolean attackPart(TestEntity entity, TestPlayer attacker, float amount, String partName) {
        return entity.definition().behaviour().attackedPart(entityContext(entity), Optional.ofNullable(attacker), amount,
                part(entity, partName));
    }

    private static lv.cebbys.mcmods.modularity.api.entity.EntityPart part(TestEntity entity, String name) {
        return entity.definition().parts().stream().filter(p -> p.name().equals(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(entity.typeId() + " has no part '" + name + "'"));
    }

    /** {@code attacker} hits {@code entity}. */
    public boolean attack(TestEntity entity, TestPlayer attacker, float amount) {
        return entity.definition().behaviour().attacked(entityContext(entity), Optional.ofNullable(attacker), amount);
    }

    /** {@code player} uses the held item on the {@code face} of the block at {@code clicked}. */
    public InteractionResult useItemOn(TestLevel level, TestPlayer player, Pos clicked, Direction face, Vec3 hit) {
        ItemStackRef stack = player.mainHandItem();
        ItemHandle handle = items.get(stack.itemId());
        if (handle == null) {
            return InteractionResult.PASS;
        }
        ItemUseContext ctx = new ItemUseContext() {
            @Override
            public Level level() {
                return level;
            }

            @Override
            public Optional<lv.cebbys.mcmods.modularity.api.world.Player> player() {
                return Optional.of(player);
            }

            @Override
            public ItemStackRef stack() {
                return stack;
            }

            @Override
            public Pos clickedPos() {
                return clicked;
            }

            @Override
            public Direction clickedFace() {
                return face;
            }

            @Override
            public Vec3 clickLocation() {
                return hit;
            }
        };
        return handle.definition().behaviour().useOnBlock(ctx);
    }

    /**
     * Simulates saving the world and loading it again: returns a new level with the same blocks
     * and entities re-created only from their saved state (unsaved data is lost, riders dismount).
     */
    public TestLevel saveAndReload(TestLevel level) {
        TestLevel reloaded = new TestLevel(this, level.isClientSide());
        level.copyBlocksTo(reloaded);
        for (TestEntity entity : level.entities()) {
            if (entity.definition() == null) {
                continue;
            }
            TestEntity copy = new TestEntity(entity.uuid(), entity.typeId(), entity.definition());
            copy.load(entity.save());
            reloaded.addEntity(copy);
        }
        return reloaded;
    }

    private EntityContext entityContext(TestEntity entity) {
        return () -> entity;
    }

    /** The player controlled by the simulated client (receives client messages, sends input). */
    public void setLocalPlayer(TestPlayer player) {
        this.localPlayer = player;
    }

    public void setMovement(MovementInput movement) {
        this.movement = movement;
    }

    public TestKeyBinding keyBinding(String name) {
        TestKeyBinding binding = keyBindings.get(name);
        if (binding == null) {
            throw new IllegalArgumentException("No key binding '" + name + "', have " + keyBindings.keySet());
        }
        return binding;
    }

    /** One client tick: runs client tick handlers and drops stopped sounds. */
    public void clientTick() {
        ClientState state = clientState();
        tickHandlers.forEach(handler -> handler.accept(state));
        playingSounds.removeIf(sound -> sound.isStopped() || sound.entity().isRemoved());
    }

    public List<LoopingSound> playingSounds() {
        return List.copyOf(playingSounds);
    }

    /** Camera settings the registered controllers ask for. */
    public Optional<CameraController.CameraSettings> camera() {
        if (localPlayer == null) {
            return Optional.empty();
        }
        for (CameraController controller : cameraControllers) {
            var settings = controller.camera(localPlayer);
            if (settings.isPresent()) {
                return settings;
            }
        }
        return Optional.empty();
    }

    /** What the HUD layers draw on a {@code width x height} screen. */
    public List<String> renderHud(int width, int height) {
        List<String> drawn = new ArrayList<>();
        HudContext ctx = new HudContext() {
            @Override
            public int width() {
                return width;
            }

            @Override
            public int height() {
                return height;
            }

            @Override
            public int textWidth(String text) {
                return text.length() * 6;
            }

            @Override
            public void text(String text, int x, int y, int color) {
                drawn.add(text);
            }

            @Override
            public void fill(int x1, int y1, int x2, int y2, int color) {
            }

            @Override
            public ClientState client() {
                return clientState();
            }
        };
        hudLayers.forEach(layer -> layer.render(ctx));
        return drawn;
    }

    /** Operations the entity renderer performs, e.g. {@code ["push", "rotateY 90.0", "model car/body"]}. */
    public List<String> renderEntity(EntityRef entity, float partialTick) {
        EntityRenderer renderer = entityRenderers.get(entity.typeId());
        if (renderer == null) {
            throw new IllegalStateException("No renderer for " + entity.typeId());
        }
        List<String> ops = new ArrayList<>();
        renderer.render(entity, partialTick, new RenderOutput() {
            @Override
            public void push() {
                ops.add("push");
            }

            @Override
            public void pop() {
                ops.add("pop");
            }

            @Override
            public void translate(double x, double y, double z) {
                ops.add("translate " + x + " " + y + " " + z);
            }

            @Override
            public void rotateX(float degrees) {
                ops.add("rotateX " + degrees);
            }

            @Override
            public void rotateY(float degrees) {
                ops.add("rotateY " + degrees);
            }

            @Override
            public void rotateZ(float degrees) {
                ops.add("rotateZ " + degrees);
            }

            @Override
            public void rotate(lv.cebbys.mcmods.modularity.api.world.Rotation r) {
                ops.add("rotate " + r.x() + " " + r.y() + " " + r.z() + " " + r.w());
            }

            @Override
            public void scale(float x, float y, float z) {
                ops.add("scale " + x + " " + y + " " + z);
            }

            @Override
            public void model(ModelRef model, Id texture) {
                model.model();
                ops.add("model " + model.resource().path());
            }

            @Override
            public void model(ModelRef model, String group, Id texture) {
                model.model().faces(group);
                ops.add("model " + model.resource().path() + "#" + group);
            }
        });
        return ops;
    }

    private ClientState clientState() {
        return new ClientState() {
            @Override
            public Optional<lv.cebbys.mcmods.modularity.api.world.Player> player() {
                return Optional.ofNullable(localPlayer);
            }

            @Override
            public MovementInput movement() {
                return movement;
            }

            @Override
            public boolean isPaused() {
                return false;
            }
        };
    }

    // ---------------------------------------------------------------- contexts

    private final class TestModContext implements CommonContext {
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

        @Override
        public EntityHandle entity(String name, Consumer<EntityDefinition.Builder> spec) {
            EntityDefinition.Builder builder = EntityDefinition.builder();
            spec.accept(builder);
            EntityDefinition definition = builder.build();
            Id id = id(name);
            EntityHandle handle = new EntityHandle() {
                @Override
                public Id id() {
                    return id;
                }

                @Override
                public EntityDefinition definition() {
                    return definition;
                }
            };
            if (entityTypes.putIfAbsent(id, handle) != null) {
                throw new IllegalStateException("Duplicate entity " + id);
            }
            return handle;
        }

        @Override
        public ItemHandle item(String name, Consumer<ItemDefinition.Builder> spec) {
            ItemDefinition.Builder builder = ItemDefinition.builder();
            spec.accept(builder);
            ItemDefinition definition = builder.build();
            Id id = id(name);
            ItemHandle handle = new ItemHandle() {
                @Override
                public Id id() {
                    return id;
                }

                @Override
                public ItemDefinition definition() {
                    return definition;
                }

                @Override
                public lv.cebbys.mcmods.modularity.api.item.ItemStackRef stack(int count) {
                    return TestItemStack.item(id, count, definition.stackSize());
                }
            };
            if (items.putIfAbsent(id, handle) != null) {
                throw new IllegalStateException("Duplicate item " + id);
            }
            return handle;
        }

        @Override
        public SoundHandle sound(String name) {
            Id id = id(name);
            SoundHandle handle = () -> id;
            sounds.put(id, handle);
            return handle;
        }

        @Override
        public <T> Channel<T> channel(String name, DataCodec<T> codec) {
            TestChannel<T> channel = new TestChannel<>(id(name), codec);
            if (channels.putIfAbsent(channel.id(), channel) != null) {
                throw new IllegalStateException("Duplicate channel " + channel.id());
            }
            return channel;
        }
    }

    /** Loopback channel: messages are serialized through the codec and delivered immediately. */
    private final class TestChannel<T> implements Channel<T> {
        private final Id id;
        private final DataCodec<T> codec;
        private final List<java.util.function.BiConsumer<lv.cebbys.mcmods.modularity.api.world.Player, T>> serverHandlers = new ArrayList<>();
        private final List<Consumer<T>> clientHandlers = new ArrayList<>();

        private TestChannel(Id id, DataCodec<T> codec) {
            this.id = id;
            this.codec = codec;
        }

        private T roundTrip(T message) {
            MapData data = new MapData();
            codec.write(message, data);
            return codec.read(data);
        }

        @Override
        public Id id() {
            return id;
        }

        @Override
        public void sendToServer(T message) {
            if (localPlayer == null) {
                throw new IllegalStateException("No local player, call TestGame#setLocalPlayer");
            }
            T received = roundTrip(message);
            serverHandlers.forEach(handler -> handler.accept(localPlayer, received));
        }

        @Override
        public void sendTo(lv.cebbys.mcmods.modularity.api.world.Player player, T message) {
            if (player == localPlayer) {
                T received = roundTrip(message);
                clientHandlers.forEach(handler -> handler.accept(received));
            }
        }

        @Override
        public void onServer(java.util.function.BiConsumer<lv.cebbys.mcmods.modularity.api.world.Player, T> handler) {
            serverHandlers.add(handler);
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

        @Override
        public void entityRenderer(EntityHandle type, EntityRenderer renderer) {
            entityRenderers.put(type.id(), renderer);
        }

        @Override
        public ModelRef objModel(Id resource) {
            return new ModelRef() {
                private ObjModel model;

                @Override
                public Id resource() {
                    return resource;
                }

                @Override
                public ObjModel model() {
                    if (model == null) {
                        String path = "assets/" + resource.namespace() + "/" + resource.path();
                        var stream = TestGame.class.getClassLoader().getResourceAsStream(path);
                        if (stream == null) {
                            throw new IllegalStateException("Model resource not found on the classpath: " + path);
                        }
                        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                            model = ObjParser.parse(reader);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                    return model;
                }
            };
        }

        @Override
        public KeyBinding keyBinding(String name, int defaultKey) {
            TestKeyBinding binding = new TestKeyBinding(name, defaultKey);
            keyBindings.put(name, binding);
            return binding;
        }

        @Override
        public void onTick(Consumer<ClientState> handler) {
            tickHandlers.add(handler);
        }

        @Override
        public <T> void onMessage(Channel<T> channel, Consumer<T> handler) {
            ((TestChannel<T>) channel).clientHandlers.add(handler);
        }

        @Override
        public void camera(CameraController controller) {
            cameraControllers.add(controller);
        }

        @Override
        public void hud(HudLayer layer) {
            hudLayers.add(layer);
        }

        @Override
        public void play(LoopingSound sound) {
            playingSounds.add(sound);
        }

        @Override
        public ClientState state() {
            return clientState();
        }
    }

    /** Key binding whose state tests control. */
    public static final class TestKeyBinding implements KeyBinding {
        private final String name;
        private final int defaultKey;
        private boolean down;
        private int clicks;

        TestKeyBinding(String name, int defaultKey) {
            this.name = name;
            this.defaultKey = defaultKey;
        }

        public int defaultKey() {
            return defaultKey;
        }

        public void setDown(boolean down) {
            this.down = down;
        }

        public void press() {
            clicks++;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public boolean isDown() {
            return down;
        }

        @Override
        public boolean consumeClick() {
            if (clicks > 0) {
                clicks--;
                return true;
            }
            return false;
        }
    }

    private record TestServerContext(String modId) implements ServerContext {
        @Override
        public Platform platform() {
            return Modularity.platform();
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
