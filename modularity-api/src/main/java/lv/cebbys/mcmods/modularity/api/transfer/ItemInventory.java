package lv.cebbys.mcmods.modularity.api.transfer;

import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;

/**
 * Fixed-size item storage. Platforms expose it through the loader's standard item transfer API
 * so hoppers, pipes and other mods can insert and extract.
 */
public interface ItemInventory {
    int size();

    ItemStackRef get(int slot);

    void set(int slot, ItemStackRef stack);

    /** Inserts as much as possible, returns what did not fit. */
    ItemStackRef insert(ItemStackRef stack);

    default boolean isEmpty() {
        for (int i = 0; i < size(); i++) {
            if (!get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
