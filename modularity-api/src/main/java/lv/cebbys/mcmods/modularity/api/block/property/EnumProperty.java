package lv.cebbys.mcmods.modularity.api.block.property;

import java.util.List;

public final class EnumProperty<E extends Enum<E> & PropertyValue> extends Property<E> {
    private final Class<E> type;
    private final List<E> values;

    EnumProperty(String name, Class<E> type, String vanillaKey) {
        super(name, vanillaKey);
        this.type = type;
        this.values = List.of(type.getEnumConstants());
    }

    @Override
    public List<E> values() {
        return values;
    }

    @Override
    public Class<E> valueType() {
        return type;
    }
}
