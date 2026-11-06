package lv.cebbys.mcmods.modularity.platform.compat;

import net.minecraft.nbt.CompoundTag;

/**
 * Reading NBT values; a missing or mistyped value reads as empty / zero / false. 1.21.5: the plain getters return
 * {@code Optional}, the {@code ...Or} getters keep the old defaults.
 */
public final class Nbt {
    private Nbt() {
    }

    public static CompoundTag compound(CompoundTag tag, String key) {
        return tag.getCompoundOrEmpty(key);
    }

    public static int getInt(CompoundTag tag, String key) {
        return tag.getIntOr(key, 0);
    }

    public static long getLong(CompoundTag tag, String key) {
        return tag.getLongOr(key, 0L);
    }

    public static double getDouble(CompoundTag tag, String key) {
        return tag.getDoubleOr(key, 0.0D);
    }

    public static boolean getBoolean(CompoundTag tag, String key) {
        return tag.getBooleanOr(key, false);
    }

    public static String getString(CompoundTag tag, String key) {
        return tag.getStringOr(key, "");
    }
}
