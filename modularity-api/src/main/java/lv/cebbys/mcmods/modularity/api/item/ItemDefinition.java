package lv.cebbys.mcmods.modularity.api.item;

import java.util.Objects;

/**
 * Everything a platform needs to create an item.
 *
 * <pre>{@code
 * mod.item("car", i -> i.stackSize(1).creativeTab(CreativeTab.TOOLS_AND_UTILITIES).behaviour(new PlaceCar()));
 * }</pre>
 */
public final class ItemDefinition {
    private final int stackSize;
    private final CreativeTab creativeTab;
    private final ItemBehaviour behaviour;

    private ItemDefinition(Builder b) {
        this.stackSize = b.stackSize;
        this.creativeTab = b.creativeTab;
        this.behaviour = b.behaviour;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int stackSize() {
        return stackSize;
    }

    /** {@code null} when the item is not listed in a creative tab. */
    public CreativeTab creativeTab() {
        return creativeTab;
    }

    public ItemBehaviour behaviour() {
        return behaviour;
    }

    public static final class Builder {
        private int stackSize = 64;
        private CreativeTab creativeTab;
        private ItemBehaviour behaviour = ItemBehaviour.NONE;

        private Builder() {
        }

        public Builder stackSize(int stackSize) {
            if (stackSize < 1 || stackSize > 99) {
                throw new IllegalArgumentException("Stack size must be 1..99");
            }
            this.stackSize = stackSize;
            return this;
        }

        public Builder creativeTab(CreativeTab tab) {
            this.creativeTab = Objects.requireNonNull(tab, "tab");
            return this;
        }

        public Builder behaviour(ItemBehaviour behaviour) {
            this.behaviour = Objects.requireNonNull(behaviour, "behaviour");
            return this;
        }

        public ItemDefinition build() {
            return new ItemDefinition(this);
        }
    }
}
