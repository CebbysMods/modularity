package lv.cebbys.mcmods.modularity.platform.compat;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

/** Item lists as NBT, in vanilla's container format. */
public final class Inventories {
    private Inventories() {
    }

    public static CompoundTag save(NonNullList<ItemStack> items, HolderLookup.Provider registries) {
        return ContainerHelper.saveAllItems(new CompoundTag(), items, registries);
    }

    public static void load(CompoundTag tag, NonNullList<ItemStack> items, HolderLookup.Provider registries) {
        ContainerHelper.loadAllItems(tag, items, registries);
    }
}
