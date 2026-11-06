package lv.cebbys.mcmods.modularity.fabric.compat;

import net.minecraft.nbt.CompoundTag;

/** Reading NBT values; a missing or mistyped value reads as empty / zero / false. */
public final class Nbt {
    private Nbt() {
    }

    public static CompoundTag compound(CompoundTag tag, String key) {
        return tag.getCompound(key);
    }

    public static int getInt(CompoundTag tag, String key) {
        return tag.getInt(key);
    }

    public static long getLong(CompoundTag tag, String key) {
        return tag.getLong(key);
    }

    public static double getDouble(CompoundTag tag, String key) {
        return tag.getDouble(key);
    }

    public static boolean getBoolean(CompoundTag tag, String key) {
        return tag.getBoolean(key);
    }

    public static String getString(CompoundTag tag, String key) {
        return tag.getString(key);
    }
}
