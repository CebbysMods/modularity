package lv.cebbys.mcmods.modularity.testkit;

import lv.cebbys.mcmods.modularity.api.block.BlockStateRef;
import lv.cebbys.mcmods.modularity.api.data.BlockData;
import lv.cebbys.mcmods.modularity.api.data.MapData;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.api.world.Level;
import lv.cebbys.mcmods.modularity.api.world.Pos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * In-memory world. Block data is stored serialized (through the codec) exactly like a platform
 * would, so codec bugs show up in tests.
 */
public final class TestLevel implements Level {
    private final TestGame game;
    private final boolean clientSide;
    private final Map<Pos, BlockStateRef> blocks = new HashMap<>();
    private final Map<Pos, Map<BlockData<?>, MapData>> data = new HashMap<>();
    private final Set<Pos> obstructed = new HashSet<>();
    private final List<Drop> drops = new ArrayList<>();
    private final List<BlockStateRef> breakEffects = new ArrayList<>();

    TestLevel(TestGame game, boolean clientSide) {
        this.game = game;
        this.clientSide = clientSide;
    }

    public record Drop(Pos pos, ItemStackRef stack) {
    }

    public TestLevel set(Pos pos, BlockStateRef state) {
        BlockStateRef previous = blockState(pos);
        if (!previous.block().equals(state.block())) {
            data.remove(pos);
        }
        if (state.isAir()) {
            blocks.remove(pos);
        } else {
            blocks.put(pos, state);
        }
        return this;
    }

    /** Marks {@code pos} as occupied by an entity, so placement there is obstructed. */
    public TestLevel obstruct(Pos pos) {
        obstructed.add(pos);
        return this;
    }

    public List<Drop> drops() {
        return drops;
    }

    /** States passed to {@code BreakContext#breakEffects}. */
    public List<BlockStateRef> breakEffects() {
        return breakEffects;
    }

    @Override
    public BlockStateRef blockState(Pos pos) {
        BlockStateRef state = blocks.get(pos);
        return state != null ? state : game.air().defaultState();
    }

    @Override
    public <T> Optional<T> data(Pos pos, BlockData<T> key) {
        Map<BlockData<?>, MapData> values = data.get(pos);
        if (values == null || !values.containsKey(key)) {
            return Optional.empty();
        }
        return Optional.of(key.codec().read(values.get(key)));
    }

    Map<BlockData<?>, Object> snapshot(Pos pos) {
        Map<BlockData<?>, Object> result = new LinkedHashMap<>();
        Map<BlockData<?>, MapData> values = data.getOrDefault(pos, Map.of());
        values.forEach((key, raw) -> result.put(key, key.codec().read(raw)));
        return result;
    }

    @Override
    public boolean isClientSide() {
        return clientSide;
    }

    @Override
    public boolean isUnobstructed(BlockStateRef state, Pos pos) {
        return !obstructed.contains(pos);
    }

    @Override
    public <T> void setData(Pos pos, BlockData<T> key, T value) {
        TestBlock block = (TestBlock) blockState(pos).block();
        if (block.definition() == null || !block.definition().data().contains(key)) {
            throw new IllegalStateException(block + " at " + pos + " does not declare " + key);
        }
        MapData raw = new MapData();
        key.codec().write(value, raw);
        data.computeIfAbsent(pos, p -> new LinkedHashMap<>()).put(key, raw);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    void setRawData(Pos pos, Map<BlockData<?>, Object> values) {
        values.forEach((key, value) -> setData(pos, (BlockData) key, value));
    }

    @Override
    public void dropItem(Pos pos, ItemStackRef stack) {
        if (!clientSide && !stack.isEmpty()) {
            drops.add(new Drop(pos, stack));
        }
    }

    void playBreakEffects(BlockStateRef state) {
        breakEffects.add(state);
    }

    @Override
    public Object handle() {
        return this;
    }
}
