package lv.cebbys.mcmods.modularity.testkit;

import lv.cebbys.mcmods.modularity.api.block.BlockRef;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;

import java.util.Objects;
import java.util.Optional;

/** Mutable item stack, like vanilla's. */
public final class TestItemStack implements ItemStackRef {
    public static final TestItemStack EMPTY = new TestItemStack(Id.minecraft("air"), 0, null, 64);

    private final Id itemId;
    private final BlockRef block;
    private final int maxStackSize;
    private int count;

    public TestItemStack(Id itemId, int count, BlockRef block, int maxStackSize) {
        this.itemId = Objects.requireNonNull(itemId, "itemId");
        this.count = count;
        this.block = block;
        this.maxStackSize = maxStackSize;
    }

    public static TestItemStack of(BlockRef block, int count) {
        return new TestItemStack(block.id(), count, block, 64);
    }

    /** A non-block item, e.g. a tool or a bucket. */
    public static TestItemStack item(Id id) {
        return new TestItemStack(id, 1, null, 1);
    }

    public static TestItemStack item(Id id, int count, int maxStackSize) {
        return new TestItemStack(id, count, null, maxStackSize);
    }

    @Override
    public boolean isEmpty() {
        return count <= 0 || itemId.equals(Id.minecraft("air"));
    }

    @Override
    public int count() {
        return count;
    }

    @Override
    public Id itemId() {
        return itemId;
    }

    @Override
    public Optional<BlockRef> block() {
        return Optional.ofNullable(block);
    }

    @Override
    public ItemStackRef withCount(int newCount) {
        return new TestItemStack(itemId, newCount, block, maxStackSize);
    }

    @Override
    public int maxStackSize() {
        return maxStackSize;
    }

    @Override
    public void shrink(int amount) {
        if (this == EMPTY) {
            return;
        }
        count = Math.max(0, count - amount);
    }

    @Override
    public Object handle() {
        return this;
    }

    @Override
    public String toString() {
        return isEmpty() ? "EMPTY" : count + "x" + itemId;
    }
}
