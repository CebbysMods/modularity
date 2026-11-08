package lv.cebbys.mcmods.modularity.fabric.block;

import lv.cebbys.mcmods.modularity.api.data.DataInput;
import lv.cebbys.mcmods.modularity.api.data.DataOutput;
import net.minecraft.nbt.CompoundTag;

/** Portable data I/O backed by NBT. */
record NbtData(CompoundTag tag) implements DataOutput, DataInput {
    @Override
    public DataOutput putString(String key, String value) {
        tag.putString(key, value);
        return this;
    }

    @Override
    public DataOutput putInt(String key, int value) {
        tag.putInt(key, value);
        return this;
    }

    @Override
    public DataOutput putBoolean(String key, boolean value) {
        tag.putBoolean(key, value);
        return this;
    }

    @Override
    public boolean has(String key) {
        return tag.contains(key);
    }

    @Override
    public String getString(String key) {
        return tag.getString(key);
    }

    @Override
    public int getInt(String key) {
        return tag.getInt(key);
    }

    @Override
    public boolean getBoolean(String key) {
        return tag.getBoolean(key);
    }
}
