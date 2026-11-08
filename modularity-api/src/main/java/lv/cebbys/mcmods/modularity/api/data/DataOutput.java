package lv.cebbys.mcmods.modularity.api.data;

import lv.cebbys.mcmods.modularity.api.core.Id;

public interface DataOutput {
    DataOutput putString(String key, String value);

    DataOutput putInt(String key, int value);

    DataOutput putBoolean(String key, boolean value);

    DataOutput putDouble(String key, double value);

    DataOutput putLong(String key, long value);

    default DataOutput putId(String key, Id value) {
        return putString(key, value.toString());
    }
}
