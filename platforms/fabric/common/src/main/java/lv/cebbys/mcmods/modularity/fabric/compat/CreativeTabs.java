package lv.cebbys.mcmods.modularity.fabric.compat;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

/** Adding items to vanilla creative tabs. */
public final class CreativeTabs {
    private CreativeTabs() {
    }

    public static void add(ResourceKey<CreativeModeTab> tab, Item item) {
        ItemGroupEvents.modifyEntriesEvent(tab).register(entries -> entries.accept(item));
    }
}
