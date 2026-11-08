package lv.cebbys.mcmods.modularity.api.block.property;

import java.util.List;

public final class BooleanProperty extends Property<Boolean> {
    private static final List<Boolean> VALUES = List.of(Boolean.FALSE, Boolean.TRUE);

    BooleanProperty(String name, String vanillaKey) {
        super(name, vanillaKey);
    }

    @Override
    public List<Boolean> values() {
        return VALUES;
    }

    @Override
    public Class<Boolean> valueType() {
        return Boolean.class;
    }
}
