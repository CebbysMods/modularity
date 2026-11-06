package lv.cebbys.mcmods.modularity.platform;

import java.util.List;
import java.util.Optional;

/** Native property for portable enums: values are their serialized names. 1.21.2: value list and index. */
final class NamedValuesProperty extends net.minecraft.world.level.block.state.properties.Property<String> {
    private final List<String> values;

    NamedValuesProperty(String name, List<String> values) {
        super(name, String.class);
        this.values = List.copyOf(values);
    }

    @Override
    public List<String> getPossibleValues() {
        return values;
    }

    @Override
    public int getInternalIndex(String value) {
        return values.indexOf(value);
    }

    @Override
    public String getName(String value) {
        return value;
    }

    @Override
    public Optional<String> getValue(String name) {
        return values.contains(name) ? Optional.of(name) : Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof NamedValuesProperty other && super.equals(o) && values.equals(other.values);
    }

    @Override
    public int generateHashCode() {
        return 31 * super.generateHashCode() + values.hashCode();
    }
}
