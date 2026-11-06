package lv.cebbys.mcmods.modularity.fabric.world;

import lv.cebbys.mcmods.modularity.api.block.BlockRef;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.fabric.Wrap;
import lv.cebbys.mcmods.modularity.fabric.compat.Ids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public record FabricItemStack(ItemStack stack) implements ItemStackRef {
    @Override
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    @Override
    public int count() {
        return stack.getCount();
    }

    @Override
    public Id itemId() {
        return Ids.id(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    @Override
    public Optional<BlockRef> block() {
        return stack.getItem() instanceof BlockItem blockItem ? Optional.of(Wrap.block(blockItem.getBlock())) : Optional.empty();
    }

    @Override
    public ItemStackRef withCount(int count) {
        return new FabricItemStack(stack.copyWithCount(count));
    }

    @Override
    public int maxStackSize() {
        return stack.getMaxStackSize();
    }

    @Override
    public void shrink(int amount) {
        stack.shrink(amount);
    }

    @Override
    public Object handle() {
        return stack;
    }
}
