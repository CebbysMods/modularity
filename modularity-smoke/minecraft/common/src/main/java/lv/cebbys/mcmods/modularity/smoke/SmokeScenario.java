package lv.cebbys.mcmods.modularity.smoke;

import lv.cebbys.mcmods.modularity.api.block.BlockHandle;
import lv.cebbys.mcmods.modularity.platform.entity.ModularEntity;
import lv.cebbys.mcmods.modularity.platform.loader.Loaders;
import lv.cebbys.mcmods.modularity.platform.transfer.McFluidTank;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Puts the smoke block in the world of the client run and mines it once. The dynamic model, the hit particles and the
 * mixins behind them need a real block at a real position, so this class supplies one.
 *
 * <p>The block appears at head height, three blocks in front of the player. The player looks at it and holds the mine
 * button, which is the same path as a player mining by hand: the hit test, the break progress and the "block being
 * mined" particles. The player is aimed again on every mining tick, and the game gives the mouse back before the hold,
 * so a moving mouse cannot turn the player away from the block.</p>
 *
 * <p>The placement runs on the server thread, because a level belongs to its own thread and a placement from the
 * client thread can be lost. The hold waits until the client sees the block, so it never mines the terrain behind it.</p>
 *
 * <p>The calls that change a block move between Minecraft versions, and the smoke sources compile for every version
 * of the table. This class therefore finds its methods by name and logs a clear error when a version has none. A red
 * smoke names the missing method instead of failing to compile.</p>
 */
final class SmokeScenario {
    private static final Logger LOGGER = LoggerFactory.getLogger("modularity-smoke");

    /** Tick at which the block appears; the client has a world and a player by then. */
    private static final int PLACE_TICK = 20;
    /** First and last tick of the mining hold. A hand needs about 30 ticks to break a block of hardness 1. */
    private static final int MINE_FROM = 30;
    private static final int MINE_TO = 110;

    private static int ticks;
    private static volatile boolean placed;
    private static boolean placementFailed;
    private static boolean mined;
    private static boolean seen;
    private static boolean waited;
    private static boolean mouseReleased;
    /** Ticks between two steps of the storage proof, so the bucket reaches the client before the right-click. */
    private static final int STORAGE_DELAY = 10;
    /** The three right-clicks: fill from a full bucket, take it out, then try to take it out again. */
    private static final Item[] STORAGE_HANDS = {Items.WATER_BUCKET, Items.BUCKET, Items.BUCKET};
    private static int storageStep = -1;
    /** 0 before the click of a step, 1 after it. */
    private static int storagePhase;
    private static int storageTicks;
    private static boolean storageDone;
    private static volatile BlockPos pos;

    private SmokeScenario() {
    }

    /** One client tick: place the block, then hold the mine button on it for a while. */
    static void tick(BlockHandle block) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || block == null) {
            return;
        }
        ticks++;
        if (ticks == 1) {
            // The game takes the mouse when the run starts. Give it back, so a moving mouse cannot turn the player
            // away from the block that this scenario aims at.
            releaseMouse(minecraft);
        }
        if (ticks == PLACE_TICK && !placed) {
            place(minecraft, block);
        }
        if (placed && ticks >= MINE_FROM && ticks <= MINE_TO && !mined) {
            mine(minecraft);
        }
    }

    private static void place(Minecraft minecraft, BlockHandle block) {
        // Head height, three blocks in front: a level view reaches it, and it stands clear of the floor.
        BlockPos target = minecraft.player.blockPosition().offset(0, 1, 3);
        pos = target;
        BlockState state = ((Block) block.get().handle()).defaultBlockState();
        MinecraftServer server = singleplayerServer(minecraft);
        if (server != null) {
            // The server thread owns the level, so it does the placement and the client gets a normal block update.
            server.execute(() -> setBlock(server.overworld(), target, state, block));
        } else {
            setBlock(minecraft.level, target, state, block);
        }
        aim(minecraft, target);
    }

    private static void setBlock(Level level, BlockPos target, BlockState state, BlockHandle block) {
        try {
            Method set = level.getClass().getMethod("setBlockAndUpdate", BlockPos.class, BlockState.class);
            set.invoke(level, target, state);
            placed = true;
            LOGGER.info("Smoke scenario: placed {} at {}", block.id(), target.toShortString());
        } catch (ReflectiveOperationException e) {
            placementFailed = true;
            LOGGER.error("Smoke scenario: this Minecraft version has no setBlockAndUpdate", e);
        }
    }

    /**
     * Holds the mine button. Vanilla does the rest: it targets whatever the player looks at, advances the break
     * progress and spawns the "block being mined" particles, which is the in-game proof of the hit-particle handler.
     * The particles come from {@code Minecraft.continueAttack}, not from {@code gameMode.continueDestroyBlock}, so the
     * scenario must take the same path as a player who holds the button.
     */
    private static void mine(Minecraft minecraft) {
        BlockPos target = pos;
        if (target == null) {
            return;
        }
        if (minecraft.level.getBlockState(target).isAir()) {
            // The block has not reached the client yet, or it is already gone. A hold now would hit the terrain
            // behind it, so the hold waits for the block and ends when the block disappears.
            if (seen) {
                mined = true;
            } else if (!waited) {
                waited = true;
                LOGGER.info("Smoke scenario: waiting for the block on the client");
            }
            return;
        }
        seen = true;
        // Aim again on every tick: the hit test uses the current camera, and a released mouse still lets the window
        // receive movement from a trackpad or a stray click.
        aim(minecraft, target);
        try {
            Method method = minecraft.getClass().getDeclaredMethod("continueAttack", boolean.class);
            method.setAccessible(true);
            method.invoke(minecraft, true);
        } catch (ReflectiveOperationException e) {
            mined = true;
            LOGGER.error("Smoke scenario: this Minecraft version has no continueAttack", e);
        }
    }

    /** Releases the mouse grab of the game window, so mouse movement no longer turns the player. */
    private static void releaseMouse(Minecraft minecraft) {
        if (mouseReleased) {
            return;
        }
        mouseReleased = true;
        try {
            Object handler = minecraft.getClass().getField("mouseHandler").get(minecraft);
            if (handler == null) {
                return;
            }
            handler.getClass().getMethod("releaseMouse").invoke(handler);
            LOGGER.info("Smoke scenario: mouse released");
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Smoke scenario: this Minecraft version has no releaseMouse", e);
        }
    }

    /** Turns the player toward the block, so the vanilla hit test of {@link #mine} finds it. */
    private static void aim(Minecraft minecraft, BlockPos target) {
        try {
            double dx = target.getX() + 0.5D - minecraft.player.getX();
            double dz = target.getZ() + 0.5D - minecraft.player.getZ();
            double dy = target.getY() + 0.5D - minecraft.player.getEyeY();
            double flat = Math.sqrt(dx * dx + dz * dz);
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float pitch = (float) -Math.toDegrees(Math.atan2(dy, flat));
            minecraft.player.getClass().getMethod("setYRot", float.class).invoke(minecraft.player, yaw);
            minecraft.player.getClass().getMethod("setXRot", float.class).invoke(minecraft.player, pitch);
        } catch (ReflectiveOperationException e) {
            LOGGER.error("Smoke scenario: this Minecraft version has no setYRot or setXRot", e);
        }
    }

    /**
     * Proves the storage methods that need a world (docs/07 7.6), with real player actions. Three right-clicks run in
     * order on the spawned entity: a water bucket fills its tank through the loader item path ({@code
     * fillFromHeldItem}), an empty bucket takes the water out through the loader lookup, and a second empty bucket must
     * move nothing, because the tank is empty. Ten ticks later the lookup stores 500 mB, which proves
     * {@code exposeEntityStorages} with a value of its own.
     */
    static void storage(boolean entityRendered) {
        if (storageDone || !entityRendered) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.gameMode == null) {
            return;
        }
        MinecraftServer server = singleplayerServer(minecraft);
        if (server == null) {
            return;
        }
        if (storageStep < 0) {
            storageStep = 0;
            armOnServer(server, STORAGE_HANDS[0]);
            storageTicks = 0;
            return;
        }
        if (++storageTicks < STORAGE_DELAY) {
            return;
        }
        storageTicks = 0;
        if (storagePhase == 0) {
            // A normal player action: the item in the main hand is used on the entity in front of the player.
            ModularEntity entity = nearestEntity(minecraft);
            if (entity != null) {
                minecraft.gameMode.interact(minecraft.player, entity, InteractionHand.MAIN_HAND);
            }
            storagePhase = 1;
            return;
        }
        if (storageStep + 1 < STORAGE_HANDS.length) {
            storageStep++;
            storagePhase = 0;
            armOnServer(server, STORAGE_HANDS[storageStep]);
            return;
        }
        readThroughLookup(server);
        storageDone = true;
    }

    private static void armOnServer(MinecraftServer server, Item hand) {
        server.execute(() -> {
            try {
                var players = server.getPlayerList().getPlayers();
                if (players.isEmpty()) {
                    LOGGER.error("Smoke storage: no server player for the right-click");
                    return;
                }
                var player = players.get(0);
                var found = server.overworld().getEntitiesOfClass(ModularEntity.class,
                        player.getBoundingBox().inflate(32.0));
                if (found.isEmpty()) {
                    LOGGER.error("Smoke storage: no smoke entity to right-click");
                    return;
                }
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(hand));
                SmokeStorageProof.arm(player, found.get(0));
            } catch (RuntimeException e) {
                LOGGER.error("Smoke storage: the right-click could not be prepared", e);
            }
        });
    }

    private static void readThroughLookup(MinecraftServer server) {
        server.execute(() -> {
            try {
                var players = server.getPlayerList().getPlayers();
                var player = players.get(0);
                var found = server.overworld().getEntitiesOfClass(ModularEntity.class,
                        player.getBoundingBox().inflate(32.0));
                if (found.isEmpty()) {
                    LOGGER.error("Smoke storage: no smoke entity to read through the lookup");
                    return;
                }
                long stored = LoaderStorageProbes.store(found.get(0), 500);
                LOGGER.info("Smoke storage: exposed {} mB", stored);
            } catch (RuntimeException e) {
                LOGGER.error("Smoke storage: lookup read failed", e);
            }
        });
    }

    private static ModularEntity nearestEntity(Minecraft minecraft) {
        var found = minecraft.level.getEntitiesOfClass(ModularEntity.class,
                minecraft.player.getBoundingBox().inflate(32.0));
        return found.isEmpty() ? null : found.get(0);
    }

    /** The integrated server of the client run; {@code null} on a remote server. */
    private static MinecraftServer singleplayerServer(Minecraft minecraft) {
        try {
            Method method = minecraft.getClass().getMethod("getSingleplayerServer");
            Object value = method.invoke(minecraft);
            if (value instanceof MinecraftServer server) {
                return server;
            }
        } catch (ReflectiveOperationException e) {
            LOGGER.debug("Smoke scenario: no single-player server method, placing in the client level", e);
        }
        return null;
    }
}
