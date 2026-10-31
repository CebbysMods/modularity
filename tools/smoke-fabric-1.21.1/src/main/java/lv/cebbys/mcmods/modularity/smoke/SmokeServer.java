package lv.cebbys.mcmods.modularity.smoke;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * On a dedicated server: places double slabs the way a player does (BlockItem#place on an existing
 * slab), logs the result, configures spawn/time, then saves and stops the server.
 */
public final class SmokeServer implements ModInitializer {
    static final Logger LOGGER = LoggerFactory.getLogger("modularity-smoke");
    private static final Block[][] PAIRS = {
            {Blocks.OAK_SLAB, Blocks.STONE_SLAB},
            {Blocks.BIRCH_SLAB, Blocks.COBBLESTONE_SLAB},
            {Blocks.QUARTZ_SLAB, Blocks.DARK_OAK_SLAB},
            {Blocks.SANDSTONE_SLAB, Blocks.PRISMARINE_SLAB},
            {Blocks.SPRUCE_SLAB, Blocks.MOSSY_COBBLESTONE_SLAB},
            {Blocks.CHERRY_SLAB, Blocks.DEEPSLATE_TILE_SLAB},
            {Blocks.RED_NETHER_BRICK_SLAB, Blocks.SMOOTH_STONE_SLAB},
    };

    private int ticks;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (server.isDedicatedServer()) {
                setUp(server);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.isDedicatedServer() && ++ticks == 40) {
                LOGGER.info("[smoke] stopping server");
                server.halt(false);
            }
        });
    }

    private static void setUp(MinecraftServer server) {
        ServerLevel level = server.overworld();
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
        int ok = 0;
        for (int i = 0; i < PAIRS.length; i++) {
            BlockPos pos = new BlockPos(-6 + i * 2, y, 4);
            ok += combine(level, pos, PAIRS[i][0], PAIRS[i][1], i % 2 == 0) ? 1 : 0;
            // a second, stacked row for depth
            ok += combine(level, pos.above().south(), PAIRS[(i + 3) % PAIRS.length][0], PAIRS[i][1], true) ? 1 : 0;
        }
        LOGGER.info("[smoke] double slabs placed: {}/{}", ok, PAIRS.length * 2);

        level.setDefaultSpawnPos(new BlockPos(0, y, -2), 0.0F);
        server.getGameRules().getRule(GameRules.RULE_SPAWN_RADIUS).set(0, server);
        server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        server.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
    }

    /** Puts {@code existing} as a slab, then "uses" a {@code held} slab on it like a player would. */
    private static boolean combine(ServerLevel level, BlockPos pos, Block existing, Block held, boolean existingIsBottom) {
        SlabType type = existingIsBottom ? SlabType.BOTTOM : SlabType.TOP;
        level.setBlockAndUpdate(pos, existing.defaultBlockState().setValue(SlabBlock.TYPE, type));
        Direction face = existingIsBottom ? Direction.UP : Direction.DOWN;
        ItemStack stack = new ItemStack(held);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false);
        var result = ((BlockItem) stack.getItem()).place(new BlockPlaceContext(level, null, InteractionHand.MAIN_HAND, stack, hit));

        var state = level.getBlockState(pos);
        var entity = level.getBlockEntity(pos);
        String data = entity == null ? "<no block entity>" : entity.saveWithoutMetadata(level.registryAccess()).toString();
        boolean combined = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals("refabslab:double_slab_block");
        LOGGER.info("[smoke] {} + {} at {} -> {} {} data={}", BuiltInRegistries.BLOCK.getKey(existing),
                BuiltInRegistries.BLOCK.getKey(held), pos.toShortString(), result, state, data);
        return combined;
    }
}
