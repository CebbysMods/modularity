package lv.cebbys.mcmods.modularity.testkit;

import lv.cebbys.mcmods.modularity.api.block.BlockDefinition;
import lv.cebbys.mcmods.modularity.api.block.BlockRef;
import lv.cebbys.mcmods.modularity.api.block.BlockStateRef;
import lv.cebbys.mcmods.modularity.api.block.BlockTag;
import lv.cebbys.mcmods.modularity.api.block.BlockType;
import lv.cebbys.mcmods.modularity.api.block.property.Property;
import lv.cebbys.mcmods.modularity.api.block.property.VanillaProperties;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** A block living in the {@link TestGame} registry. */
public final class TestBlock implements BlockRef {
    private final Id id;
    private final Set<BlockType> types;
    private final Map<Property<?>, Object> properties;
    private final Set<BlockTag> tags = new HashSet<>();
    private final BlockDefinition definition;
    private float hardness;
    private int lightEmission;
    private boolean hasItem;

    TestBlock(Id id, Set<BlockType> types, Map<Property<?>, Object> properties, BlockDefinition definition) {
        this.id = id;
        this.types = types.isEmpty() ? EnumSet.noneOf(BlockType.class) : EnumSet.copyOf(types);
        this.properties = new LinkedHashMap<>(properties);
        this.definition = definition;
        this.hardness = definition == null ? 1.5F : definition.hardness();
        this.hasItem = definition == null || definition.hasItem();
    }

    /** A vanilla-like slab block: {@code type=bottom} by default. */
    static TestBlock slab(Id id) {
        Map<Property<?>, Object> properties = new LinkedHashMap<>();
        properties.put(VanillaProperties.SLAB_TYPE, lv.cebbys.mcmods.modularity.api.block.property.SlabType.BOTTOM);
        TestBlock block = new TestBlock(id, EnumSet.of(BlockType.SLAB), properties, null);
        block.tags.add(BlockTag.SLABS);
        return block;
    }

    static TestBlock simple(Id id) {
        return new TestBlock(id, Set.of(), Map.of(), null);
    }

    public TestBlock hardness(float hardness) {
        this.hardness = hardness;
        return this;
    }

    public TestBlock lightEmission(int lightEmission) {
        this.lightEmission = lightEmission;
        return this;
    }

    public TestBlock tag(BlockTag tag) {
        tags.add(tag);
        return this;
    }

    public TestBlock noItem() {
        this.hasItem = false;
        return this;
    }

    /** Definition when this block was registered through a mod, otherwise {@code null}. */
    public BlockDefinition definition() {
        return definition;
    }

    Map<Property<?>, Object> defaultValues() {
        return properties;
    }

    float hardness() {
        return hardness;
    }

    int baseLightEmission() {
        return lightEmission;
    }

    Set<BlockTag> tags() {
        return tags;
    }

    @Override
    public Id id() {
        return id;
    }

    @Override
    public TestBlockState defaultState() {
        return new TestBlockState(this, properties);
    }

    @Override
    public boolean is(BlockType type) {
        return types.contains(type);
    }

    @Override
    public ItemStackRef asStack(int count) {
        return hasItem ? TestItemStack.of(this, count) : TestItemStack.EMPTY;
    }

    @Override
    public Object handle() {
        return this;
    }

    @Override
    public String toString() {
        return "Block{" + id + "}";
    }

    BlockStateRef stateOf(Map<Property<?>, Object> values) {
        return new TestBlockState(this, values);
    }
}
