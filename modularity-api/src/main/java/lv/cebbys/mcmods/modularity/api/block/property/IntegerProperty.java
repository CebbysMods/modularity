package lv.cebbys.mcmods.modularity.api.block.property;

import java.util.List;
import java.util.stream.IntStream;

public final class IntegerProperty extends Property<Integer> {
    private final int min;
    private final int max;
    private final List<Integer> values;

    IntegerProperty(String name, int min, int max, String vanillaKey) {
        super(name, vanillaKey);
        if (min < 0 || max <= min) {
            throw new IllegalArgumentException("Invalid integer property range [" + min + ", " + max + "]");
        }
        this.min = min;
        this.max = max;
        this.values = IntStream.rangeClosed(min, max).boxed().toList();
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    @Override
    public List<Integer> values() {
        return values;
    }

    @Override
    public Class<Integer> valueType() {
        return Integer.class;
    }
}
