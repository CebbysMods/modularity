package lv.cebbys.mcmods.modularity.fabric.block;

import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.data.DataCodec;
import lv.cebbys.mcmods.modularity.api.data.DataInput;
import lv.cebbys.mcmods.modularity.api.data.DataOutput;
import lv.cebbys.mcmods.modularity.fabric.compat.Nbt;
import net.minecraft.nbt.CompoundTag;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/** Portable data I/O backed by NBT. */
public record NbtData(CompoundTag tag) implements DataOutput, DataInput {

    /** {@code value} written by {@code codec} into a new compound. */
    public static <T> CompoundTag encode(DataCodec<T> codec, Object value) {
        CompoundTag out = new CompoundTag();
        @SuppressWarnings("unchecked") T typed = (T) value;
        codec.write(typed, new NbtData(out));
        return out;
    }

    /** {@code tag} read by {@code codec}; null reads as empty. */
    public static <T> T decode(DataCodec<T> codec, CompoundTag tag) {
        return codec.read(new NbtData(tag == null ? new CompoundTag() : tag));
    }

    /**
     * Typed values ({@code BlockData}, {@code EntityData}) under their key ids: each value in its own compound.
     * {@code include} selects the keys (saved, synced).
     */
    public static <K> CompoundTag writeAll(Map<K, Object> values, Function<K, Id> id,
                                           Function<K, DataCodec<?>> codec, Predicate<K> include) {
        CompoundTag out = new CompoundTag();
        values.forEach((key, value) -> {
            if (include.test(key)) {
                out.put(id.apply(key).toString(), encode(codec.apply(key), value));
            }
        });
        return out;
    }

    /** Reads the {@code include}d {@code keys} present in {@code in} into {@code values}. */
    public static <K> void readAll(Iterable<? extends K> keys, Function<K, Id> id, Function<K, DataCodec<?>> codec,
                                   Predicate<K> include, CompoundTag in, Map<K, Object> values) {
        for (K key : keys) {
            String name = id.apply(key).toString();
            if (include.test(key) && in.contains(name)) {
                values.put(key, decode(codec.apply(key), Nbt.compound(in, name)));
            }
        }
    }
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
    public DataOutput putDouble(String key, double value) {
        tag.putDouble(key, value);
        return this;
    }

    @Override
    public DataOutput putLong(String key, long value) {
        tag.putLong(key, value);
        return this;
    }

    @Override
    public double getDouble(String key) {
        return Nbt.getDouble(tag, key);
    }

    @Override
    public long getLong(String key) {
        return Nbt.getLong(tag, key);
    }

    @Override
    public boolean has(String key) {
        return tag.contains(key);
    }

    @Override
    public String getString(String key) {
        return Nbt.getString(tag, key);
    }

    @Override
    public int getInt(String key) {
        return Nbt.getInt(tag, key);
    }

    @Override
    public boolean getBoolean(String key) {
        return Nbt.getBoolean(tag, key);
    }
}
