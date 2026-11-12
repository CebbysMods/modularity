package lv.cebbys.mcmods.modularity.loom.compat;

import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.world.Container;

/** Transfer API storages over vanilla objects. 26.1: {@code InventoryStorage} is {@code ContainerStorage}. */
public final class Storages {
    private Storages() {
    }

    /** All slots of {@code container}, from no particular side. */
    public static Storage<ItemVariant> of(Container container) {
        return ContainerStorage.of(container, null);
    }
}
