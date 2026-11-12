package lv.cebbys.mcmods.modularity.loom.compat;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.world.Container;

/** Transfer API storages over vanilla objects. */
public final class Storages {
    private Storages() {
    }

    /** All slots of {@code container}, from no particular side. */
    public static Storage<ItemVariant> of(Container container) {
        return InventoryStorage.of(container, null);
    }
}
