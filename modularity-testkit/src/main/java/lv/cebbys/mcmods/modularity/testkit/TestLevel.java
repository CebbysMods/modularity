package lv.cebbys.mcmods.modularity.testkit;

import lv.cebbys.mcmods.modularity.api.block.BlockStateRef;
import lv.cebbys.mcmods.modularity.api.block.property.SlabType;
import lv.cebbys.mcmods.modularity.api.block.property.VanillaProperties;
import lv.cebbys.mcmods.modularity.api.entity.EntityHandle;
import lv.cebbys.mcmods.modularity.api.entity.EntityRef;
import lv.cebbys.mcmods.modularity.api.world.Aabb;
import lv.cebbys.mcmods.modularity.api.world.Vec3;
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
    private final List<TestEntity> entities = new ArrayList<>();
    private final List<ItemDrop> itemDrops = new ArrayList<>();
    long gameTime;

    TestLevel(TestGame game, boolean clientSide) {
        this.game = game;
        this.clientSide = clientSide;
    }

    public record Drop(Pos pos, ItemStackRef stack) {
    }

    /** An item dropped at an exact position. */
    public record ItemDrop(Vec3 at, ItemStackRef stack) {
    }

    /** Fills the box {@code from..to} (inclusive) with {@code state}. */
    public TestLevel fill(Pos from, Pos to, BlockStateRef state) {
        for (int x = Math.min(from.x(), to.x()); x <= Math.max(from.x(), to.x()); x++) {
            for (int y = Math.min(from.y(), to.y()); y <= Math.max(from.y(), to.y()); y++) {
                for (int z = Math.min(from.z(), to.z()); z <= Math.max(from.z(), to.z()); z++) {
                    set(new Pos(x, y, z), state);
                }
            }
        }
        return this;
    }

    /** Modularity entities and players in this level. */
    public List<TestEntity> entities() {
        return List.copyOf(entities);
    }

    public List<ItemDrop> itemDrops() {
        return itemDrops;
    }

    /** Puts a player into this level. */
    public TestPlayer add(TestPlayer player, Vec3 position) {
        player.level = this;
        player.setPosition(position);
        if (!entities.contains(player)) {
            entities.add(player);
        }
        return player;
    }

    void addEntity(TestEntity entity) {
        entity.level = this;
        entities.add(entity);
    }

    void removeEntity(TestEntity entity) {
        entities.remove(entity);
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

    void copyBlocksTo(TestLevel other) {
        other.blocks.putAll(blocks);
        other.gameTime = gameTime;
        data.forEach((pos, values) -> other.data.put(pos, new LinkedHashMap<>(values)));
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
    public void dropItem(Vec3 at, ItemStackRef stack) {
        if (!clientSide && !stack.isEmpty()) {
            itemDrops.add(new ItemDrop(at, stack));
        }
    }

    /** Full cubes for every block, half cubes for slabs, nothing for air. */
    @Override
    public List<Aabb> blockCollisions(Aabb area) {
        List<Aabb> boxes = new ArrayList<>();
        int minX = (int) Math.floor(area.minX()), maxX = (int) Math.floor(area.maxX());
        int minY = (int) Math.floor(area.minY()), maxY = (int) Math.floor(area.maxY());
        int minZ = (int) Math.floor(area.minZ()), maxZ = (int) Math.floor(area.maxZ());
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockStateRef state = blocks.get(new Pos(x, y, z));
                    if (state == null || state.isAir()) {
                        continue;
                    }
                    double bottom = y;
                    double top = y + 1;
                    if (state.has(VanillaProperties.SLAB_TYPE)) {
                        SlabType type = state.get(VanillaProperties.SLAB_TYPE);
                        if (type == SlabType.BOTTOM) {
                            top = y + 0.5;
                        } else if (type == SlabType.TOP) {
                            bottom = y + 0.5;
                        }
                    }
                    Aabb box = new Aabb(x, bottom, z, x + 1, top, z + 1);
                    if (box.intersects(area)) {
                        boxes.add(box);
                    }
                }
            }
        }
        return boxes;
    }

    @Override
    public EntityRef spawn(EntityHandle type, Vec3 position, float yaw) {
        if (clientSide) {
            throw new IllegalStateException("Entities are spawned on the server");
        }
        TestEntity entity = new TestEntity(java.util.UUID.randomUUID(), type.id(), type.definition());
        entity.setPosition(position);
        entity.setYaw(yaw);
        addEntity(entity);
        return entity;
    }

    /**
     * Mirrors {@code entity} into this client level (same uuid, position and synced data), like a
     * client receiving it from the server. Use {@code TestGame#tick} on this level to run client ticks.
     */
    public TestEntity mirror(EntityRef serverEntity) {
        if (!clientSide) {
            throw new IllegalStateException("Only client levels mirror entities");
        }
        TestEntity source = (TestEntity) serverEntity;
        TestEntity copy = entities.stream().filter(e -> e.uuid().equals(source.uuid())).findFirst()
                .orElseGet(() -> {
                    TestEntity created = new TestEntity(source.uuid(), source.typeId(), source.definition());
                    addEntity(created);
                    return created;
                });
        copy.syncFrom(source);
        return copy;
    }

    @Override
    public long gameTime() {
        return gameTime;
    }

    @Override
    public Object handle() {
        return this;
    }
}
