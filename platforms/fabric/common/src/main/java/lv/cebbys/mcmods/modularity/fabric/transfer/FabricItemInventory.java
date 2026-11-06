package lv.cebbys.mcmods.modularity.fabric.transfer;

import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.api.transfer.ItemInventory;
import lv.cebbys.mcmods.modularity.fabric.Wrap;
import lv.cebbys.mcmods.modularity.fabric.compat.Storages;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.world.SimpleContainer;

/** Portable inventory over a vanilla container, exposed to other mods as a Transfer API storage. */
public final class FabricItemInventory implements ItemInventory {
    private final SimpleContainer container;
    private final Storage<ItemVariant> storage;

    public FabricItemInventory(int slots) {
        this.container = new SimpleContainer(slots);
        this.storage = Storages.of(container);
    }

    public SimpleContainer container() {
        return container;
    }

    public Storage<ItemVariant> storage() {
        return storage;
    }

    @Override
    public int size() {
        return container.getContainerSize();
    }

    @Override
    public ItemStackRef get(int slot) {
        return Wrap.stack(container.getItem(slot));
    }

    @Override
    public void set(int slot, ItemStackRef stack) {
        container.setItem(slot, Wrap.stack(stack).copy());
    }

    @Override
    public ItemStackRef insert(ItemStackRef stack) {
        return Wrap.stack(container.addItem(Wrap.stack(stack).copy()));
    }
}
