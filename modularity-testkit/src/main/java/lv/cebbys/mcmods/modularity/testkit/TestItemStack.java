package lv.cebbys.mcmods.modularity.testkit;

import lv.cebbys.mcmods.modularity.api.block.BlockRef;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;

import java.util.Optional;

public record TestItemStack(Id itemId, int count, BlockRef blockOrNull) implements ItemStackRef {
    public static final TestItemStack EMPTY = new TestItemStack(Id.minecraft("air"), 0, null);

    public static TestItemStack of(BlockRef block, int count) {
        return new TestItemStack(block.id(), count, block);
    }

    /** A non-block item, e.g. a tool. */
    public static TestItemStack item(Id id) {
        return new TestItemStack(id, 1, null);
    }

    @Override
    public boolean isEmpty() {
        return count <= 0;
    }

    @Override
    public Optional<BlockRef> block() {
        return Optional.ofNullable(blockOrNull);
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
