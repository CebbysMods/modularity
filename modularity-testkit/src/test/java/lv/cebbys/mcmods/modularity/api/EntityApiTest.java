package lv.cebbys.mcmods.modularity.api;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import lv.cebbys.mcmods.modularity.api.client.CameraController;
import lv.cebbys.mcmods.modularity.api.client.ClientContext;
import lv.cebbys.mcmods.modularity.api.client.KeyBinding;
import lv.cebbys.mcmods.modularity.api.client.Keys;
import lv.cebbys.mcmods.modularity.api.client.LoopingSound;
import lv.cebbys.mcmods.modularity.api.client.ModelRef;
import lv.cebbys.mcmods.modularity.api.client.MovementInput;
import lv.cebbys.mcmods.modularity.api.client.model.ObjModel;
import lv.cebbys.mcmods.modularity.api.client.model.ObjParser;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.core.InteractionResult;
import lv.cebbys.mcmods.modularity.api.data.DataCodec;
import lv.cebbys.mcmods.modularity.api.entity.EntityBehaviour;
import lv.cebbys.mcmods.modularity.api.entity.EntityContext;
import lv.cebbys.mcmods.modularity.api.entity.EntityData;
import lv.cebbys.mcmods.modularity.api.entity.EntityHandle;
import lv.cebbys.mcmods.modularity.api.entity.EntityRef;
import lv.cebbys.mcmods.modularity.api.item.ItemBehaviour;
import lv.cebbys.mcmods.modularity.api.item.ItemHandle;
import lv.cebbys.mcmods.modularity.api.item.ItemUseContext;
import lv.cebbys.mcmods.modularity.api.network.Channel;
import lv.cebbys.mcmods.modularity.api.sound.SoundHandle;
import lv.cebbys.mcmods.modularity.api.world.Aabb;
import lv.cebbys.mcmods.modularity.api.world.Direction;
import lv.cebbys.mcmods.modularity.api.world.Player;
import lv.cebbys.mcmods.modularity.api.world.Pos;
import lv.cebbys.mcmods.modularity.api.world.Vec3;
import lv.cebbys.mcmods.modularity.testkit.TestEntity;
import lv.cebbys.mcmods.modularity.testkit.TestGame;
import lv.cebbys.mcmods.modularity.testkit.TestItemStack;
import lv.cebbys.mcmods.modularity.testkit.TestLevel;
import lv.cebbys.mcmods.modularity.testkit.TestPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;
import java.util.Optional;

import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Epics;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Features;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Issues;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Stories;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Tags;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Entities, items, channels and client hooks on the test platform. */
@Epic(Epics.MODULARITY)
@Feature(Features.L1_UNIT)
@Story(Stories.TESTKIT_ENTITIES)
@Issue(Issues.PR_9)
@Tag(Tags.PR_9)
class EntityApiTest {
    static final EntityData<Integer> TICKS = EntityData.of(Id.of("testmod", "ticks"), DataCodec.INT, 0);
    static final EntityData<Integer> INPUT = EntityData.of(Id.of("testmod", "input"), DataCodec.INT, 0).notSaved().notSynced();

    /** A mod with client work only: it implements the client stage and overrides nothing common. */
    static final class ClientOnlyMod implements ModularityModCommon, ModularityModClient {
        ClientContext client;

        @Override
        public void initialize(ClientContext client) {
            this.client = client;
        }
    }

    /** A tiny mod: a cart that counts ticks, can be ridden, is placed by an item and steered by a channel. */
    static final class CartMod implements ModularityModCommon, ModularityModClient {
        EntityHandle cart;
        ItemHandle cartItem;
        SoundHandle hum;
        Channel<Integer> steer;
        KeyBinding honk;
        ModelRef model;
        int honks;

        @Override
        public void initialize(CommonContext common) {
            cart = common.entity("cart", e -> e.size(1.0F, 0.8F)
                    .seat(new Vec3(0, 0.5, 0))
                    .data(TICKS, INPUT)
                    .fluidTank(2000, Id.minecraft("lava"))
                    .inventory(3)
                    .behaviour(new EntityBehaviour() {
                        @Override
                        public void tick(EntityContext ctx) {
                            EntityRef e = ctx.entity();
                            e.setData(TICKS, e.data(TICKS) + 1);
                            e.setPosition(e.position().add(e.data(INPUT), 0, 0));
                        }

                        @Override
                        public InteractionResult interact(EntityContext ctx, Player player) {
                            if (player.fillFromHeldItem(ctx.entity().fluidTank().orElseThrow()) > 0) {
                                return InteractionResult.SUCCESS;
                            }
                            return ctx.entity().addPassenger(player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
                        }
                    }));
            cartItem = common.item("cart", i -> i.stackSize(1).behaviour(new ItemBehaviour() {
                @Override
                public InteractionResult useOnBlock(ItemUseContext ctx) {
                    Pos above = ctx.clickedPos().offset(ctx.clickedFace());
                    ctx.level().spawn(cart, new Vec3(above.x() + 0.5, above.y(), above.z() + 0.5), 90);
                    ctx.stack().shrink(1);
                    return InteractionResult.SUCCESS;
                }
            }));
            hum = common.sound("hum");
            steer = common.channel("steer", DataCodec.INT);
            steer.onServer((player, value) -> player.vehicle().ifPresent(v -> v.setData(INPUT, value)));
        }

        @Override
        public void initialize(ClientContext client) {
            honk = client.keyBinding("honk", Keys.H);
            model = client.objModel(client.id("models/cube.obj"));
            client.onTick(state -> {
                if (honk.consumeClick()) {
                    honks++;
                }
                state.player().filter(p -> p.vehicle().isPresent())
                        .ifPresent(p -> steer.sendToServer(state.movement().forward() ? 1 : 0));
            });
            client.camera(player -> player.vehicle().map(v -> new CameraController.CameraSettings(true, 6.0F)));
            client.hud(ctx -> ctx.client().player().flatMap(Player::vehicle)
                    .ifPresent(v -> ctx.text("ticks " + v.data(TICKS), 2, 2, 0xFFFFFF)));
            client.entityRenderer(cart, (entity, partial, out) -> {
                out.push();
                out.rotateY(180 - entity.yaw());
                out.model(model, "front", client.id("textures/cart.png"));
                out.pop();
            });
        }
    }

    private TestGame game;
    private CartMod mod;
    private TestLevel level;
    private TestPlayer player;

    @BeforeEach
    void setUp() {
        game = TestGame.create();
        game.block("minecraft:stone");
        mod = new CartMod();
        game.load("testmod", mod);
        level = game.level();
        level.fill(new Pos(-5, 63, -5), new Pos(5, 63, 5), game.block(Id.minecraft("stone")).orElseThrow().defaultState());
        player = level.add(new TestPlayer(), new Vec3(0.5, 64, 0.5));
        game.setLocalPlayer(player);
    }

    private TestEntity placeCart() {
        player.holding(mod.cartItem.stack(1));
        assertEquals(InteractionResult.SUCCESS, game.useItemOn(level, player, new Pos(0, 63, 0), Direction.UP, new Vec3(0.5, 64, 0.5)));
        return level.entities().stream().filter(e -> e.is(mod.cart)).findFirst().orElseThrow();
    }

    @Test
    void itemSpawnsEntityAndIsConsumed() {
        TestEntity cart = placeCart();
        assertEquals(new Vec3(0.5, 64, 0.5), cart.position());
        assertEquals(90.0F, cart.yaw());
        assertTrue(player.mainHandItem().isEmpty());
    }

    @Test
    void entityTicksAndKeepsSavedDataOnReload() {
        TestEntity cart = placeCart();
        cart.setData(INPUT, 0);
        game.tick(level, 5);
        assertEquals(5, cart.data(TICKS));

        cart.setData(INPUT, 3);
        TestLevel reloaded = game.saveAndReload(level);
        TestEntity loaded = reloaded.entities().stream().filter(e -> e.is(mod.cart)).findFirst().orElseThrow();
        assertEquals(5, loaded.data(TICKS), "saved data survives");
        assertEquals(0, loaded.data(INPUT), "notSaved data is reset");
        assertEquals(cart.position(), loaded.position());
    }

    @Test
    void ridingRespectsSeatCount() {
        TestEntity cart = placeCart();
        game.interact(cart, player);
        assertEquals(List.of(player), cart.passengers());
        assertEquals(Optional.of(cart), player.vehicle());
        assertFalse(cart.addPassenger(level.add(new TestPlayer(), Vec3.ZERO)), "only one seat");
        player.dismount();
        assertTrue(cart.passengers().isEmpty());
    }

    @Test
    void fluidTankFilledFromBucketAndInventoryMerges() {
        TestEntity cart = placeCart();
        player.holding(TestItemStack.item(Id.minecraft("lava_bucket")));
        game.interact(cart, player);
        assertEquals(1000, cart.fluidTank().orElseThrow().amount());
        assertEquals(Id.minecraft("bucket"), player.mainHandItem().itemId());
        assertTrue(cart.passengers().isEmpty(), "refuelling does not mount");

        player.holding(TestItemStack.item(Id.minecraft("water_bucket")));
        game.interact(cart, player);
        assertEquals(1000, cart.fluidTank().orElseThrow().amount(), "water is not accepted");

        var inventory = cart.inventory().orElseThrow();
        var rest = inventory.insert(TestItemStack.item(Id.minecraft("coal"), 200, 64));
        assertEquals(8, rest.count(), "3 slots x 64");
    }

    @Test
    void channelCarriesClientInputToServer() {
        TestEntity cart = placeCart();
        game.interact(cart, player);
        game.setMovement(new MovementInput(true, false, false, false, false, false));
        game.clientTick();
        assertEquals(1, cart.data(INPUT));
        game.tick(level);
        assertEquals(1.5, cart.position().x(), 1e-9);
    }

    @Test
    void clientHooks() {
        TestEntity cart = placeCart();
        assertTrue(game.camera().isEmpty());
        game.interact(cart, player);
        assertEquals(Optional.of(new CameraController.CameraSettings(true, 6.0F)), game.camera());

        game.tick(level, 2);
        assertEquals(List.of("ticks 2"), game.renderHud(320, 240));

        game.keyBinding("honk").press();
        game.clientTick();
        game.clientTick();
        assertEquals(1, mod.honks);
        assertEquals(Keys.H, game.keyBinding("honk").defaultKey());

        assertEquals(List.of("push", "rotateY 90.0", "model models/cube.obj#front", "pop"), game.renderEntity(cart, 0.0F));
    }

    @Test
    void loopingSoundsStopWithTheirEntity() {
        TestEntity cart = placeCart();
        ClientOnlyMod clientOnly = new ClientOnlyMod();
        TestGame other = TestGame.create();
        other.load("other", clientOnly);
        clientOnly.client.play(new LoopingSound() {
            @Override
            public SoundHandle sound() {
                return mod.hum;
            }

            @Override
            public EntityRef entity() {
                return cart;
            }

            @Override
            public boolean isStopped() {
                return false;
            }

            @Override
            public float volume() {
                return 1;
            }

            @Override
            public float pitch() {
                return 1;
            }
        });
        other.clientTick();
        assertEquals(1, other.playingSounds().size());
        cart.remove();
        other.clientTick();
        assertTrue(other.playingSounds().isEmpty());
    }

    @Test
    void blockCollisionsUseRealShapes() {
        var slab = game.slab("minecraft:oak_slab");
        level.set(new Pos(2, 64, 0), slab.defaultState());
        List<Aabb> boxes = level.blockCollisions(new Aabb(1.9, 63.9, 0.1, 3.0, 65, 0.9));
        assertTrue(boxes.contains(new Aabb(2, 64, 0, 3, 64.5, 1)), "bottom slab is half a block");
        assertTrue(boxes.contains(new Aabb(2, 63, 0, 3, 64, 1)), "stone floor");
        assertTrue(level.blockCollisions(new Aabb(0.1, 64.1, 0.1, 0.9, 65, 0.9)).isEmpty(), "air");
    }

    @Test
    void objParserReadsGroupsAndFansPolygons() throws Exception {
        ObjModel model = mod.model.model();
        assertEquals(List.of("front", "fan"), List.copyOf(model.groupNames()));
        assertEquals(1, model.faces("front").size());
        assertEquals(4, model.faces("front").get(0).vertices().size());
        assertEquals(3, model.faces("fan").size(), "pentagon -> 3 triangles");
        assertThrows(IllegalArgumentException.class, () -> model.faces("missing"));
        assertThrows(java.io.IOException.class, () -> ObjParser.parse(new StringReader("f 1 2")));
    }

    @Test
    void yawRotationFollowsMinecraftConvention() {
        Vec3 forward = new Vec3(0, 0, 1);
        assertEquals(1.0, forward.rotateYaw(0).z(), 1e-9, "yaw 0 faces south (+z)");
        assertEquals(-1.0, forward.rotateYaw(90).x(), 1e-9, "yaw 90 faces west (-x)");
        assertEquals(1.0, new Vec3(1, 0, 0).rotateYaw(0).x(), 1e-9, "local +x is left = east when facing south");
    }
}
