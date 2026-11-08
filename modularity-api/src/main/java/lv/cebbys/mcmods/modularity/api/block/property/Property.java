package lv.cebbys.mcmods.modularity.api.block.property;

import java.util.List;
import java.util.Objects;

/**
 * Portable block state property definition. Platforms translate it into the native
 * {@code net.minecraft...Property} once and cache the mapping.
 *
 * <pre>{@code
 * static final IntegerProperty LIGHT = Property.integer("light_level", 0, 15);
 * static final BooleanProperty LIT = Property.bool("lit");
 * static final EnumProperty<Mode> MODE = Property.enumeration("mode", Mode.class);
 * }</pre>
 *
 * Vanilla properties (that other blocks already use) are available in {@link VanillaProperties}.
 */
public abstract sealed class Property<T> permits IntegerProperty, BooleanProperty, EnumProperty {
    private final String name;
    private final String vanillaKey;

    Property(String name, String vanillaKey) {
        this.name = Objects.requireNonNull(name, "name");
        this.vanillaKey = vanillaKey;
    }

    public static IntegerProperty integer(String name, int min, int max) {
        return new IntegerProperty(name, min, max, null);
    }

    public static BooleanProperty bool(String name) {
        return new BooleanProperty(name, null);
    }

    public static <E extends Enum<E> & PropertyValue> EnumProperty<E> enumeration(String name, Class<E> type) {
        return new EnumProperty<>(name, type, null);
    }

    public String name() {
        return name;
    }

    /**
     * Non-null when this property mirrors an existing vanilla property (e.g. {@code slab type}).
     * Platforms then map it to the vanilla instance instead of creating a new property.
     */
    public String vanillaKey() {
        return vanillaKey;
    }

    public abstract List<T> values();

    public abstract Class<T> valueType();

    public T defaultValue() {
        return values().get(0);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + name + "]";
    }
}
