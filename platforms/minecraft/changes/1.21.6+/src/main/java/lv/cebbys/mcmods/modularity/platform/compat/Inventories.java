package lv.cebbys.mcmods.modularity.platform.compat;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** Item lists as NBT, in vanilla's container format. 1.21.6: through tag-backed {@code ValueInput}/{@code ValueOutput}. */
public final class Inventories {
    private Inventories() {
    }

    public static CompoundTag save(NonNullList<ItemStack> items, HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        ContainerHelper.saveAllItems(output, items);
        return output.buildResult();
    }

    public static void load(CompoundTag tag, NonNullList<ItemStack> items, HolderLookup.Provider registries) {
        ContainerHelper.loadAllItems(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag), items);
    }
}
