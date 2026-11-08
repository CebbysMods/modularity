package lv.cebbys.mcmods.modularity.fabric.block;

import lv.cebbys.mcmods.modularity.api.data.BlockData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Block data decided during placement ({@code PlacementResult#with}) waits here until the block
 * entity of the placed block joins the level, then it is moved into the block entity. This runs on
 * both the client (prediction) and the server.
 */
public final class PendingData {
    private static final Map<Level, Map<BlockPos, Entry>> PENDING = Collections.synchronizedMap(new WeakHashMap<>());

    private record Entry(Block block, Map<BlockData<?>, Object> data) {
    }

    private PendingData() {
    }

    public static void put(Level level, BlockPos pos, Block block, Map<BlockData<?>, Object> data) {
        if (data.isEmpty()) {
            return;
        }
        PENDING.computeIfAbsent(level, l -> new HashMap<>()).put(pos.immutable(), new Entry(block, Map.copyOf(data)));
    }

    public static Map<BlockData<?>, Object> take(Level level, BlockPos pos, Block block) {
        Map<BlockPos, Entry> perLevel = PENDING.get(level);
        if (perLevel == null) {
            return Map.of();
        }
        Entry entry = perLevel.remove(pos);
        return entry != null && entry.block() == block ? entry.data() : Map.of();
    }
}
