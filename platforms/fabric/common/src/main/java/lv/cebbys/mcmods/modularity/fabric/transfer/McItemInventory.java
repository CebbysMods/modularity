package lv.cebbys.mcmods.modularity.fabric.transfer;

import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.api.transfer.ItemInventory;
import lv.cebbys.mcmods.modularity.fabric.Wrap;
import net.minecraft.world.SimpleContainer;

/** Portable inventory over a vanilla container; other mods reach the container through the loader's storage API. */
public final class McItemInventory implements ItemInventory {
    private final SimpleContainer container;

    public McItemInventory(int slots) {
        this.container = new SimpleContainer(slots);
    }

    public SimpleContainer container() {
        return container;
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
