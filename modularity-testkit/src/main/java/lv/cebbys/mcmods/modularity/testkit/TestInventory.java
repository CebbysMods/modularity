package lv.cebbys.mcmods.modularity.testkit;

import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.api.transfer.ItemInventory;

import java.util.Arrays;

/** In-memory {@link ItemInventory}. */
public final class TestInventory implements ItemInventory {
    private final ItemStackRef[] slots;

    public TestInventory(int size) {
        slots = new ItemStackRef[size];
        Arrays.fill(slots, TestItemStack.EMPTY);
    }

    @Override
    public int size() {
        return slots.length;
    }

    @Override
    public ItemStackRef get(int slot) {
        return slots[slot];
    }

    @Override
    public void set(int slot, ItemStackRef stack) {
        slots[slot] = stack;
    }

    @Override
    public ItemStackRef insert(ItemStackRef stack) {
        int remaining = stack.count();
        for (int i = 0; i < slots.length && remaining > 0; i++) {
            ItemStackRef slot = slots[i];
            if (!slot.isEmpty() && slot.itemId().equals(stack.itemId())) {
                int moved = Math.min(remaining, slot.maxStackSize() - slot.count());
                if (moved > 0) {
                    slots[i] = slot.withCount(slot.count() + moved);
                    remaining -= moved;
                }
            }
        }
        for (int i = 0; i < slots.length && remaining > 0; i++) {
            if (slots[i].isEmpty()) {
                int moved = Math.min(remaining, stack.maxStackSize());
                slots[i] = stack.withCount(moved);
                remaining -= moved;
            }
        }
        return remaining == 0 ? TestItemStack.EMPTY : stack.withCount(remaining);
    }
}
