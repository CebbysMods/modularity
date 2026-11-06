package lv.cebbys.mcmods.modularity.platform.block;

import lv.cebbys.mcmods.modularity.api.block.BlockInterceptor;
import lv.cebbys.mcmods.modularity.api.block.BlockSelector;
import lv.cebbys.mcmods.modularity.platform.Wrap;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Registered {@link BlockInterceptor}s, resolved and cached per native block. */
public final class Interceptors {
    private static final List<Entry> ENTRIES = new CopyOnWriteArrayList<>();
    private static final Map<Block, List<BlockInterceptor>> CACHE = new ConcurrentHashMap<>();

    private record Entry(BlockSelector selector, BlockInterceptor interceptor) {
    }

    private Interceptors() {
    }

    public static void register(BlockSelector selector, BlockInterceptor interceptor) {
        ENTRIES.add(new Entry(selector, interceptor));
        CACHE.clear();
    }

    public static boolean isEmpty() {
        return ENTRIES.isEmpty();
    }

    public static List<BlockInterceptor> forBlock(Block block) {
        return CACHE.computeIfAbsent(block, b -> {
            var portable = Wrap.block(b);
            return ENTRIES.stream()
                    .filter(entry -> entry.selector().test(portable))
                    .map(Entry::interceptor)
                    .toList();
        });
    }
}
