package lv.cebbys.mcmods.modularity.testkit;

import lv.cebbys.mcmods.modularity.api.block.BlockStateRef;
import lv.cebbys.mcmods.modularity.api.block.BlockTag;
import lv.cebbys.mcmods.modularity.api.block.property.Property;
import lv.cebbys.mcmods.modularity.api.world.BlockView;
import lv.cebbys.mcmods.modularity.api.world.Pos;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class TestBlockState implements BlockStateRef {
    private final TestBlock block;
    private final Map<Property<?>, Object> values;

    TestBlockState(TestBlock block, Map<Property<?>, Object> values) {
        this.block = block;
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    @Override
    public TestBlock block() {
        return block;
    }

    @Override
    public boolean has(Property<?> property) {
        return values.containsKey(property);
    }

    @Override
    public <T> T get(Property<T> property) {
        if (!has(property)) {
            throw new IllegalArgumentException(block + " has no property " + property);
        }
        return property.valueType().cast(values.get(property));
    }

    @Override
    public <T> BlockStateRef with(Property<T> property, T value) {
        if (!has(property)) {
            throw new IllegalArgumentException(block + " has no property " + property);
        }
        if (!property.values().contains(value)) {
            throw new IllegalArgumentException(value + " is not a value of " + property);
        }
        Map<Property<?>, Object> copy = new LinkedHashMap<>(values);
        copy.put(property, value);
        return new TestBlockState(block, copy);
    }

    @Override
    public float destroySpeed(BlockView view, Pos pos) {
        return block.hardness();
    }

    @Override
    public int lightEmission() {
        if (block.definition() != null && block.definition().lightLevel() != null) {
            return block.definition().lightLevel().applyAsInt(this);
        }
        return block.baseLightEmission();
    }

    @Override
    public boolean is(BlockTag tag) {
        return block.tags().contains(tag);
    }

    @Override
    public Object handle() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof TestBlockState other && block == other.block && values.equals(other.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(block.id(), values);
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder(block.id().toString());
        if (!values.isEmpty()) {
            out.append('[');
            values.forEach((property, value) -> out.append(property.name()).append('=').append(value).append(','));
            out.setLength(out.length() - 1);
            out.append(']');
        }
        return out.toString();
    }
}
