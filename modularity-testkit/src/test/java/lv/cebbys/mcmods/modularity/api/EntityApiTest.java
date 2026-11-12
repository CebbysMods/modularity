package lv.cebbys.mcmods.modularity.api;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Step;
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
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
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
import org.junit.jupiter.api.DisplayName;
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

/** Entities, items, channels, sounds and client hooks on the test platform. */
@Epic(Epics.MODULARITY)
@Feature(Features.UNIT)
@Story(Stories.ENTITY_API)
@Issue(Issues.PR_4)
@Tag(Tags.PR_4)
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
    private TestEntity cart;
    private TestLevel reloaded;
    private TestGame other;
    private ItemStackRef rest;
    private ObjModel model;

    @BeforeEach
    @Step("Start a test game with a stone floor and a player at the origin")
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

    @Test
    @DisplayName("Using the cart item on a block spawns the cart and consumes the stack")
    void itemSpawnsEntityAndIsConsumed() {
        placeCart();
        assertSpawn();
        assertItemConsumed();
    }

    @Test
    @DisplayName("The cart ticks and keeps its saved data through a reload")
    void entityTicksAndKeepsSavedDataOnReload() {
        placeCart();
        setInput(0);
        tickLevel(5);
        assertTicks(5);

        setInput(3);
        saveAndReload();
        assertReloadedData();
    }

    @Test
    @DisplayName("A player rides the cart and the single seat stays full")
    void ridingRespectsSeatCount() {
        placeCart();
        mount();
        assertRiding();
        assertSeatFull();
        dismount();
        assertNoPassenger();
    }

    @Test
    @DisplayName("A lava bucket fills the tank and the inventory merges stacks")
    void fluidTankFilledFromBucketAndInventoryMerges() {
        placeCart();
        useHeldItem(Id.minecraft("lava_bucket"));
        assertLavaFilled();

        useHeldItem(Id.minecraft("water_bucket"));
        assertWaterRefused();

        insertCoal();
        assertInventoryRest();
    }

    @Test
    @DisplayName("A channel carries client input to the server, which moves the cart")
    void channelCarriesClientInputToServer() {
        placeCart();
        mount();
        pressForward();
        runClientTick();
        assertInput(1);
        tickLevel(1);
        assertMoved(1.5);
    }

    @Test
    @DisplayName("Client key, camera, HUD and entity renderer hooks run for the cart")
    void clientHooks() {
        placeCart();
        assertNoCamera();
        mount();
        assertCamera();

        tickLevel(2);
        assertHud();

        pressHonk();
        assertHonk();

        assertEntityRenderer();
    }

    @Test
    @DisplayName("A looping sound stops when its entity is removed")
    void loopingSoundsStopWithTheirEntity() {
        placeCart();
        playLoopingHum();
        assertSoundCount(1);
        removeCartAndTick();
        assertSoundCount(0);
    }

    @Test
    @DisplayName("Block collisions use the real block shapes")
    void blockCollisionsUseRealShapes() {
        placeOakSlab();
        assertCollisions();
    }

    @Test
    @DisplayName("The OBJ parser reads groups and splits a polygon into triangles")
    void objParserReadsGroupsAndFansPolygons() {
        model = mod.model.model();
        assertObjGroups();
        assertObjFaces();
        assertObjErrors();
    }

    @Test
    @DisplayName("Yaw 0 faces south and yaw 90 faces west")
    void yawRotationFollowsMinecraftConvention() {
        assertYawConvention();
    }

    @Step("Check yaw 0, yaw 90 and the local x axis")
    private void assertYawConvention() {
        Vec3 forward = new Vec3(0, 0, 1);
        assertEquals(1.0, forward.rotateYaw(0).z(), 1e-9, "yaw 0 faces south (+z)");
        assertEquals(-1.0, forward.rotateYaw(90).x(), 1e-9, "yaw 90 faces west (-x)");
        assertEquals(1.0, new Vec3(1, 0, 0).rotateYaw(0).x(), 1e-9, "local +x is left = east when facing south");
    }

    @Step("Place the cart above the block under the player's feet")
    private void placeCart() {
        player.holding(mod.cartItem.stack(1));
        assertEquals(InteractionResult.SUCCESS,
                game.useItemOn(level, player, new Pos(0, 63, 0), Direction.UP, new Vec3(0.5, 64, 0.5)),
                "the item use succeeds");
        cart = level.entities().stream().filter(e -> e.is(mod.cart)).findFirst().orElseThrow();
    }

    @Step("Check the spawn position and yaw")
    private void assertSpawn() {
        assertEquals(new Vec3(0.5, 64, 0.5), cart.position());
        assertEquals(90.0F, cart.yaw());
    }

    @Step("Check that the used item stack is empty")
    private void assertItemConsumed() {
        assertTrue(player.mainHandItem().isEmpty(), "the stack is empty after the use");
    }

    @Step("Set the input data to {0}")
    private void setInput(int input) {
        cart.setData(INPUT, input);
    }

    @Step("Tick the level {0} times")
    private void tickLevel(int ticks) {
        game.tick(level, ticks);
    }

    @Step("Save the level and load it again")
    private void saveAndReload() {
        reloaded = game.saveAndReload(level);
    }

    @Step("Check the saved data, the unsaved data and the position after the reload")
    private void assertReloadedData() {
        TestEntity loaded = reloaded.entities().stream().filter(e -> e.is(mod.cart)).findFirst().orElseThrow();
        assertEquals(5, loaded.data(TICKS), "saved data survives");
        assertEquals(0, loaded.data(INPUT), "notSaved data is reset");
        assertEquals(cart.position(), loaded.position(), "the position survives");
    }

    @Step("Interact with the cart as the player")
    private void mount() {
        game.interact(cart, player);
    }

    @Step("Dismount the player")
    private void dismount() {
        player.dismount();
    }

    @Step("Check that a second player cannot take the one seat")
    private void assertSeatFull() {
        assertFalse(cart.addPassenger(level.add(new TestPlayer(), Vec3.ZERO)), "only one seat");
    }

    @Step("Hold a {0} and interact with the cart")
    private void useHeldItem(Id itemId) {
        player.holding(TestItemStack.item(itemId));
        game.interact(cart, player);
    }

    @Step("Insert 200 coal into the cart inventory")
    private void insertCoal() {
        rest = cart.inventory().orElseThrow().insert(TestItemStack.item(Id.minecraft("coal"), 200, 64));
    }

    @Step("Press forward as the client")
    private void pressForward() {
        game.setMovement(new MovementInput(true, false, false, false, false, false));
    }

    @Step("Run one client tick so the channel sends the input")
    private void runClientTick() {
        game.clientTick();
    }

    @Step("Press the honk key and run two client ticks")
    private void pressHonk() {
        game.keyBinding("honk").press();
        game.clientTick();
        game.clientTick();
    }

    @Step("Load a second game and play the hum as a looping sound of the cart")
    private void playLoopingHum() {
        ClientOnlyMod clientOnly = new ClientOnlyMod();
        other = TestGame.create();
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
    }

    @Step("Remove the cart and tick the second game client")
    private void removeCartAndTick() {
        cart.remove();
        other.clientTick();
    }

    @Step("Place an oak slab at (2, 64, 0)")
    private void placeOakSlab() {
        var slab = game.slab("minecraft:oak_slab");
        level.set(new Pos(2, 64, 0), slab.defaultState());
    }

    @Step("Check that the tick counter is {0}")
    private void assertTicks(int ticks) {
        assertEquals(ticks, cart.data(TICKS));
    }

    @Step("Check that the player is the only passenger and rides the cart")
    private void assertRiding() {
        assertEquals(List.of(player), cart.passengers());
        assertEquals(Optional.of(cart), player.vehicle());
    }

    @Step("Check that the cart has no passenger")
    private void assertNoPassenger() {
        assertTrue(cart.passengers().isEmpty());
    }

    @Step("Check the tank holds 1000 mB and the player holds an empty bucket")
    private void assertLavaFilled() {
        assertEquals(1000, cart.fluidTank().orElseThrow().amount(), "the lava bucket fills 1000 mB");
        assertEquals(Id.minecraft("bucket"), player.mainHandItem().itemId(), "the player holds an empty bucket");
        assertTrue(cart.passengers().isEmpty(), "refuelling does not mount");
    }

    @Step("Check that the tank still holds 1000 mB")
    private void assertWaterRefused() {
        assertEquals(1000, cart.fluidTank().orElseThrow().amount(), "water is not accepted");
    }

    @Step("Check that 8 coal remain after the insert")
    private void assertInventoryRest() {
        assertEquals(8, rest.count(), "3 slots x 64 leaves 8 coal");
    }

    @Step("Check that the server holds the input {0}")
    private void assertInput(int input) {
        assertEquals(input, cart.data(INPUT));
    }

    @Step("Check that the cart is at x = {0}")
    private void assertMoved(double x) {
        assertEquals(x, cart.position().x(), 1e-9);
    }

    @Step("Check that no camera is active before the mount")
    private void assertNoCamera() {
        assertTrue(game.camera().isEmpty());
    }

    @Step("Check the camera settings of the ridden cart")
    private void assertCamera() {
        assertEquals(Optional.of(new CameraController.CameraSettings(true, 6.0F)), game.camera());
    }

    @Step("Check that the HUD shows the tick counter")
    private void assertHud() {
        assertEquals(List.of("ticks 2"), game.renderHud(320, 240));
    }

    @Step("Check the honk counter and the default key")
    private void assertHonk() {
        assertEquals(1, mod.honks, "the honk binding fired once");
        assertEquals(Keys.H, game.keyBinding("honk").defaultKey(), "the default key is H");
    }

    @Step("Check the entity renderer calls in order")
    private void assertEntityRenderer() {
        assertEquals(List.of("push", "rotateY 90.0", "model models/cube.obj#front", "pop"),
                game.renderEntity(cart, 0.0F));
    }

    @Step("Check that {0} sounds are playing")
    private void assertSoundCount(int count) {
        assertEquals(count, other.playingSounds().size());
    }

    @Step("Query the collisions in the slab column and in an air column")
    private void assertCollisions() {
        List<Aabb> boxes = level.blockCollisions(new Aabb(1.9, 63.9, 0.1, 3.0, 65, 0.9));
        assertTrue(boxes.contains(new Aabb(2, 64, 0, 3, 64.5, 1)), "bottom slab is half a block");
        assertTrue(boxes.contains(new Aabb(2, 63, 0, 3, 64, 1)), "stone floor");
        assertTrue(level.blockCollisions(new Aabb(0.1, 64.1, 0.1, 0.9, 65, 0.9)).isEmpty(), "air");
    }

    @Step("Check the OBJ group names")
    private void assertObjGroups() {
        assertEquals(List.of("front", "fan"), List.copyOf(model.groupNames()));
    }

    @Step("Check the face count and the vertices")
    private void assertObjFaces() {
        assertEquals(1, model.faces("front").size(), "front is one face");
        assertEquals(4, model.faces("front").get(0).vertices().size(), "front is a quad");
        assertEquals(3, model.faces("fan").size(), "a pentagon becomes three triangles");
    }

    @Step("Check that a missing group and a two-vertex face are errors")
    private void assertObjErrors() {
        assertThrows(IllegalArgumentException.class, () -> model.faces("missing"));
        assertThrows(java.io.IOException.class, () -> ObjParser.parse(new StringReader("f 1 2")));
    }
}
