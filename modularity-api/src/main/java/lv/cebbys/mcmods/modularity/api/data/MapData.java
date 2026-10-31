package lv.cebbys.mcmods.modularity.api.data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/** Simple in-memory {@link DataOutput}/{@link DataInput}, handy for tests and for copying data. */
public final class MapData implements DataOutput, DataInput {
    private final Map<String, Object> values = new LinkedHashMap<>();

    public Map<String, Object> values() {
        return values;
    }

    @Override
    public MapData putString(String key, String value) {
        values.put(key, value);
        return this;
    }

    @Override
    public MapData putInt(String key, int value) {
        values.put(key, value);
        return this;
    }

    @Override
    public MapData putBoolean(String key, boolean value) {
        values.put(key, value);
        return this;
    }

    @Override
    public MapData putDouble(String key, double value) {
        values.put(key, value);
        return this;
    }

    @Override
    public MapData putLong(String key, long value) {
        values.put(key, value);
        return this;
    }

    @Override
    public double getDouble(String key) {
        return ((Number) require(key)).doubleValue();
    }

    @Override
    public long getLong(String key) {
        return ((Number) require(key)).longValue();
    }

    @Override
    public boolean has(String key) {
        return values.containsKey(key);
    }

    @Override
    public String getString(String key) {
        return (String) require(key);
    }

    @Override
    public int getInt(String key) {
        return (Integer) require(key);
    }

    @Override
    public boolean getBoolean(String key) {
        return (Boolean) require(key);
    }

    private Object require(String key) {
        Object value = values.get(key);
        if (value == null) {
            throw new NoSuchElementException("Missing data key '" + key + "'");
        }
        return value;
    }
}
