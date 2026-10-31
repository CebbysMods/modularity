package lv.cebbys.mcmods.modularity.api.block;

import lv.cebbys.mcmods.modularity.api.data.BlockData;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * What to place instead: a block state and, optionally, block data that is attached to the
 * position atomically with the placement (on both client prediction and server).
 */
public final class PlacementResult {
    private final BlockStateRef state;
    private final Map<BlockData<?>, Object> data;

    private PlacementResult(BlockStateRef state, Map<BlockData<?>, Object> data) {
        this.state = Objects.requireNonNull(state, "state");
        this.data = Collections.unmodifiableMap(data);
    }

    public static PlacementResult of(BlockStateRef state) {
        return new PlacementResult(state, new LinkedHashMap<>());
    }

    public <T> PlacementResult with(BlockData<T> key, T value) {
        Map<BlockData<?>, Object> copy = new LinkedHashMap<>(data);
        copy.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
        return new PlacementResult(state, copy);
    }

    public BlockStateRef state() {
        return state;
    }

    public Map<BlockData<?>, Object> data() {
        return data;
    }
}
