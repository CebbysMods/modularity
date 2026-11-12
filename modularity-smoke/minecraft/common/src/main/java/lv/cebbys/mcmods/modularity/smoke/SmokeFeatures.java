package lv.cebbys.mcmods.modularity.smoke;

import lv.cebbys.mcmods.modularity.api.CommonContext;
import lv.cebbys.mcmods.modularity.api.Modularity;
import lv.cebbys.mcmods.modularity.api.ModularityModClient;
import lv.cebbys.mcmods.modularity.api.ModularityModCommon;
import lv.cebbys.mcmods.modularity.api.ModularityModServer;
import lv.cebbys.mcmods.modularity.api.block.BlockBehaviour;
import lv.cebbys.mcmods.modularity.api.block.BlockHandle;
import lv.cebbys.mcmods.modularity.api.block.BreakContext;
import lv.cebbys.mcmods.modularity.api.client.ClientContext;
import lv.cebbys.mcmods.modularity.api.client.KeyBinding;
import lv.cebbys.mcmods.modularity.api.client.Keys;
import lv.cebbys.mcmods.modularity.api.client.ModelRef;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.data.DataCodec;
import lv.cebbys.mcmods.modularity.api.entity.EntityHandle;
import lv.cebbys.mcmods.modularity.api.core.InteractionResult;
import lv.cebbys.mcmods.modularity.api.entity.EntityBehaviour;
import lv.cebbys.mcmods.modularity.api.entity.EntityContext;
import lv.cebbys.mcmods.modularity.api.entity.EntityRef;
import lv.cebbys.mcmods.modularity.api.item.ItemHandle;
import lv.cebbys.mcmods.modularity.api.network.Channel;
import lv.cebbys.mcmods.modularity.api.server.ServerContext;
import lv.cebbys.mcmods.modularity.platform.loader.Loaders;
import lv.cebbys.mcmods.modularity.platform.transfer.McFluidTank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Feature smoke of the platform smoke tests (docs/07-multi-version-build.md, 7.6). It registers a block, an item and
 * a payload channel through the API, then resolves the block in the server stage and, on the first client tick, on the
 * client. The channel
 * carries one round trip in both directions, and a tank on the loader's unit proves the fluid conversion. The client
 * stage also registers a model decided per position, a HUD layer, a rebindable key and an entity renderer, so the
 * client seam of the loader runs in the game and not only at compile time. The block spawns the registered entity
 * when a player breaks it, so the client renderer has a real entity to draw. Each step logs one line, so a run
 * proves the registration path of the loader and not only its entry points.
 *
 * <p>Every step catches its own failure and logs it. A broken registration then shows as a missing or failed line,
 * and it never stops the server or the client.</p>
 *
 * <p>Every loader registers this mod. Fabric and Quilt discover it through the {@code "modularity"} entrypoint of
 * their metadata; NeoForge and Forge call {@link Modularity#registerMod} in the smoke mod constructor, and Modularity
 * runs the common stage on {@code NewRegistryEvent}, which fires after every mod constructor (bean
 * {@code modularity-4uol}).</p>
 */
public final class SmokeFeatures implements ModularityModCommon, ModularityModClient, ModularityModServer {
    /** The mod id of the smoke mod, as in {@code fabric.mod.json} and {@code quilt.mod.json}. */
    public static final String MOD_ID = "modularity_smoke";

    private static final Logger LOGGER = LoggerFactory.getLogger("modularity-smoke");

    private static BlockHandle block;
    private static EntityHandle entity;
    private static ItemHandle item;
    private static Channel<Integer> channel;
    /** The value the client sends, so both logs name the same message. */
    private static final int MESSAGE = 42;
    private static boolean sent;
    /** Set when the first HUD frame is drawn, so the layer logs one line and not one per frame. */
    private static boolean hudDrawn;
    /** Set when the key mapping is reported once, after the first tick with a player. */
    private static boolean keyReported;
    /** Set when the dynamic model is asked for the quads of the placed block. */
    private static boolean modelLogged;
    /** Set when the hit particles ask the handler for a particle state. */
    private static boolean particlesLogged;
    /** Set when the entity renderer of the loader draws the spawned smoke entity for the first time. */
    private static boolean entityLogged;
    /** Set when the client stage resolved the block and the fluid unit, which waits for the first tick. */
    private static boolean clientResolved;
    private static KeyBinding key;
    /** The OBJ model the client stage asks for; it parses on first use, which needs the resource manager. */
    private static ModelRef objModel;
    /** Set when the entity renderer drew the OBJ model into a frame. */
    private static boolean objDrawn;

    @Override
    public void initialize(CommonContext common) {
        try {
            // The tank makes the loader expose the entity storage to other mods (docs/07 7.6): the registration path
            // calls StorageBackend.exposeEntityStorages for a definition with a tank.
            entity = common.entity("feature_entity", definition -> definition.size(0.6F, 1.8F).fluidTank(4000)
                    .behaviour(new EntityBehaviour() {
                        @Override
                        public InteractionResult interact(EntityContext context, lv.cebbys.mcmods.modularity.api.world.Player player) {
                            // A player right-clicks the entity with a fluid container: the loader moves the fluid.
                            return SmokeStorageProof.interact(context, player);
                        }
                    }));
            // The block spawns the entity when a player breaks it. The client smoke mines the block, so the server
            // side of the loader places a real entity in the world, and the client renderer of the entity then runs.
            block = common.block("feature_block", definition -> definition.strength(1.0F, 3.0F)
                    .behaviour(new BlockBehaviour() {
                        @Override
                        public void onPlayerBreak(BreakContext context) {
                            context.breakEffects(context.state());
                            if (context.level().isClientSide()) {
                                return;
                            }
                            try {
                                EntityRef spawned = context.level().spawn(entity, context.pos().center(), 0.0F);
                                LOGGER.info("Smoke entity: spawned {} at {}", spawned.typeId(), context.pos());
                            } catch (RuntimeException e) {
                                LOGGER.error("Smoke entity: spawn failed", e);
                            }
                        }
                    }));
            item = common.item("feature_item", definition -> {
            });
            channel = common.channel("feature", DataCodec.INT);
            // Both directions in one round trip: the client sends, the server logs and answers, the client logs.
            channel.onServer((player, value) -> {
                LOGGER.info("Smoke network: server received {}", value);
                channel.sendTo(player, value);
            });
            LOGGER.info("Smoke features: declared block={} item={} channel={} entity={}",
                    block.id(), item.id(), channel.id(), entity.id());
        } catch (RuntimeException e) {
            LOGGER.error("Smoke features: declaration failed", e);
        }
    }

    @Override
    public void initialize(ClientContext client) {
        try {
            client.onMessage(channel, value -> LOGGER.info("Smoke network: client received {}", value));
            // The client seam has more than the tick handler: a model decided per position, a HUD layer and a
            // rebindable key. Each one logs a line, so a run proves that the loader wires all of them.
            client.blockModel(block, (view, pos, state, out) -> {
                if (!modelLogged) {
                    modelLogged = true;
                    LOGGER.info("Smoke model: quads at {}", pos);
                }
                out.blockState(state);
            });
            LOGGER.info("Smoke model: registered for {}", block.id());
            client.hitParticles(block, (view, pos, state, face, particle) -> {
                if (!particlesLogged) {
                    particlesLogged = true;
                    LOGGER.info("Smoke particles: hit {} at {}", state.block().id(), pos);
                }
                return state;
            });
            // The OBJ model of the smoke mod: it names a resource in this mod, and the parser reads it on first use.
            objModel = client.objModel(Id.of(MOD_ID, "models/obj/smoke_cube.obj"));
            // The entity renderer seam: the loader calls it with the spawned entity, and the renderer draws the parsed
            // OBJ cube with a vanilla texture, so the run shows a real object on the screen. The first draw logs one
            // line, which proves the geometry reached the frame and not only the parser.
            client.entityRenderer(entity, (rendered, partialTick, out) -> {
                if (!entityLogged) {
                    entityLogged = true;
                    LOGGER.info("Smoke entity: rendered {}", rendered.typeId());
                }
                out.push();
                // The model is a unit cube with its corner at the origin, and the entity stands in the centre of the
                // broken block, so the cube moves by half a block on every axis and fills exactly that block space.
                out.translate(-0.5, -0.5, -0.5);
                out.model(objModel, Id.of("minecraft", "textures/block/stone.png"));
                out.pop();
                if (!objDrawn) {
                    objDrawn = true;
                    LOGGER.info("Smoke obj: drawn {} faces={}", rendered.typeId(), objModel.model().faceCount());
                }
            });
            LOGGER.info("Smoke entity: registered for {}", entity.id());
            client.hud(context -> {
                if (hudDrawn) {
                    return;
                }
                hudDrawn = true;
                context.fill(0, 0, 8, 8, 0xFFFF00FF);
                LOGGER.info("Smoke client: hud drawn {}x{}", context.width(), context.height());
            });
            key = client.keyBinding("smoke_key", Keys.J);
            // The client stage runs before the world exists, so the send waits for the first tick with a player.
            client.onTick(state -> {
                if (state.player().isEmpty()) {
                    return;
                }
                // The client stage may run before the block registry is readable (Forge family, docs/07 7.8), so the
                // client-side resolution and the fluid unit wait for the first tick, where the game is complete.
                if (!clientResolved) {
                    clientResolved = true;
                    resolve("client");
                    // The OBJ model parses on first use against the client resource manager, so the first tick with a
                    // player is the earliest point where the resource is complete.
                    obj("client");
                }
                // The scenario places the block in the loaded world and mines it, so the model, the hit particles and
                // the mixins behind them run in the game and not only at registration.
                SmokeScenario.tick(block);
                // The two storage proofs that need a world (docs/07 7.6): the fill from the held item needs the server
                // player, and the lookup read needs the entity that the broken block spawns.
                SmokeScenario.storage(entityLogged);
                if (!keyReported) {
                    keyReported = true;
                    LOGGER.info("Smoke keys: binding={} down={}", key.name(), key.isDown());
                }
                if (sent) {
                    return;
                }
                sent = true;
                channel.sendToServer(MESSAGE);
            });
        } catch (RuntimeException e) {
            LOGGER.error("Smoke features: client setup failed", e);
        }
    }

    @Override
    public void initialize(ServerContext server) {
        resolve("server");
    }

    /** Proves that the loader registered the block: the handle resolves and the id is in the registry. */
    private static void resolve(String side) {
        try {
            if (block == null) {
                throw new IllegalStateException("the common stage did not declare the block");
            }
            var resolved = block.get();
            boolean present = Modularity.block(block.id()).isPresent();
            LOGGER.info("Smoke features: resolved {} block={} present={}", side, resolved.id(), present);
        } catch (RuntimeException e) {
            LOGGER.error("Smoke features: resolution failed on " + side, e);
        }
        storage(side);
    }

    /**
     * Proves the OBJ model path on this loader (docs/07 7.6): the model reference parses a Wavefront file out of the
     * client resources and reports the faces and groups it found. The classes behind it are shared, so a line here
     * proves the resource manager, the reload hook and the parser of the loader at once.
     */
    private static void obj(String side) {
        try {
            if (objModel == null) {
                throw new IllegalStateException("the client stage declared no OBJ model");
            }
            var model = objModel.model();
            LOGGER.info("Smoke obj: parsed {} faces={} groups={}",
                    objModel.resource(), model.faceCount(), model.groupNames().size());
        } catch (RuntimeException e) {
            LOGGER.error("Smoke obj: parse failed on " + side, e);
        }
    }

    /**
     * Proves the loader's fluid unit (docs/07 7.6): the tank keeps the loader's unit internally, so 1000 mB fills
     * exactly {@code unitsPerMb} units per millibucket, and the portable view still reports millibuckets.
     */
    private static void storage(String side) {
        try {
            long unitsPerMb = Loaders.storage().unitsPerMb();
            McFluidTank tank = new McFluidTank(unitsPerMb, 4000, Set.of(), () -> {
            });
            long inserted = tank.fill(Id.of("minecraft", "water"), 1000, false);
            long units = tank.units();
            long drained = tank.drain(400, false);
            LOGGER.info("Smoke storage: unitsPerMb={} inserted={} units={} drained={} amount={}",
                    unitsPerMb, inserted, units, drained, tank.amount());
        } catch (RuntimeException e) {
            LOGGER.error("Smoke storage: tank failed on " + side, e);
        }
    }
}
