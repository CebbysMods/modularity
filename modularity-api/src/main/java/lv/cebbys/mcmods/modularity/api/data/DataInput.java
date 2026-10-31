package lv.cebbys.mcmods.modularity.api.data;

import lv.cebbys.mcmods.modularity.api.core.Id;

public interface DataInput {
    boolean has(String key);

    String getString(String key);

    int getInt(String key);

    boolean getBoolean(String key);

    double getDouble(String key);

    long getLong(String key);

    default Id getId(String key) {
        return Id.parse(getString(key));
    }
}
