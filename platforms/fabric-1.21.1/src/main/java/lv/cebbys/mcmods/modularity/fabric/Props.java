package lv.cebbys.mcmods.modularity.fabric;

import lv.cebbys.mcmods.modularity.api.block.property.BooleanProperty;
import lv.cebbys.mcmods.modularity.api.block.property.EnumProperty;
import lv.cebbys.mcmods.modularity.api.block.property.IntegerProperty;
import lv.cebbys.mcmods.modularity.api.block.property.Property;
import lv.cebbys.mcmods.modularity.api.block.property.PropertyValue;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Maps portable properties to native ones (and their values). */
public final class Props {
    private static final Map<String, net.minecraft.world.level.block.state.properties.Property<?>> VANILLA = Map.of(
            "slab_type", BlockStateProperties.SLAB_TYPE,
            "waterlogged", BlockStateProperties.WATERLOGGED,
            "lit", BlockStateProperties.LIT,
            "powered", BlockStateProperties.POWERED,
            "power", BlockStateProperties.POWER
    );
    private static final Map<Property<?>, net.minecraft.world.level.block.state.properties.Property<?>> CACHE = new ConcurrentHashMap<>();

    private Props() {
    }

    public static net.minecraft.world.level.block.state.properties.Property<?> toNative(Property<?> property) {
        return CACHE.computeIfAbsent(property, Props::create);
    }

    private static net.minecraft.world.level.block.state.properties.Property<?> create(Property<?> property) {
        if (property.vanillaKey() != null) {
            var vanilla = VANILLA.get(property.vanillaKey());
            if (vanilla == null) {
                throw new IllegalArgumentException("Unknown vanilla property " + property.vanillaKey());
            }
            return vanilla;
        }
        if (property instanceof IntegerProperty integer) {
            return net.minecraft.world.level.block.state.properties.IntegerProperty.create(integer.name(), integer.min(), integer.max());
        }
        if (property instanceof BooleanProperty) {
            return net.minecraft.world.level.block.state.properties.BooleanProperty.create(property.name());
        }
        if (property instanceof EnumProperty<?> enumeration) {
            return new NamedValuesProperty(enumeration.name(),
                    enumeration.values().stream().map(PropertyValue::serializedName).toList());
        }
        throw new IllegalArgumentException("Unsupported property " + property);
    }

    /** Portable value to native value. */
    public static Comparable<?> toNativeValue(Property<?> property, Object value) {
        if (value instanceof PropertyValue named) {
            var nativeProperty = toNative(property);
            if (nativeProperty instanceof NamedValuesProperty) {
                return named.serializedName();
            }
            for (Object candidate : nativeProperty.getPossibleValues()) {
                if (candidate instanceof StringRepresentable representable
                        && representable.getSerializedName().equals(named.serializedName())) {
                    return (Comparable<?>) candidate;
                }
            }
            throw new IllegalArgumentException(value + " has no native counterpart in " + nativeProperty);
        }
        return (Comparable<?>) value;
    }

    /** Native value to portable value. */
    public static <T> T toPortableValue(Property<T> property, Object nativeValue) {
        if (property instanceof EnumProperty<?>) {
            String name = nativeValue instanceof StringRepresentable representable
                    ? representable.getSerializedName() : nativeValue.toString();
            for (T candidate : property.values()) {
                if (((PropertyValue) candidate).serializedName().equals(name)) {
                    return candidate;
                }
            }
            throw new IllegalArgumentException(nativeValue + " is not a value of " + property);
        }
        return property.valueType().cast(nativeValue);
    }

    /** Native property for portable enums: values are their serialized names. */
    static final class NamedValuesProperty extends net.minecraft.world.level.block.state.properties.Property<String> {
        private final List<String> values;

        NamedValuesProperty(String name, List<String> values) {
            super(name, String.class);
            this.values = List.copyOf(values);
        }

        @Override
        public Collection<String> getPossibleValues() {
            return values;
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
}
