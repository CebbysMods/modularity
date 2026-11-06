package lv.cebbys.mcmods.modularity.fabric.compat;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

/** Adding items to vanilla creative tabs. 26.1: Fabric's item group API is the creative tab API. */
public final class CreativeTabs {
    private CreativeTabs() {
    }

    public static void add(ResourceKey<CreativeModeTab> tab, Item item) {
        CreativeModeTabEvents.modifyOutputEvent(tab).register(output -> output.accept(item));
    }
}
