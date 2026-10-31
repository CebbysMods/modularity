package lv.cebbys.mcmods.modularity.api.block;

import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.api.world.Player;

import java.util.List;

/**
 * Logic of a block you define. Override only what you need, every default keeps vanilla behaviour.
 * The interface grows with each milestone (use, neighbour updates, ticking, shapes...) without
 * breaking existing implementations.
 */
public interface BlockBehaviour {
    BlockBehaviour VANILLA = new BlockBehaviour() {
    };

    /**
     * Fraction of the block broken per tick by {@code player} (vanilla {@code getDestroyProgress}).
     * Return {@link Float#NaN} to keep the vanilla value.
     */
    default float destroyProgress(BlockContext ctx, Player player) {
        return Float.NaN;
    }

    /** Called before the block is removed by a player; default plays vanilla break effects. */
    default void onPlayerBreak(BreakContext ctx) {
        ctx.breakEffects(ctx.state());
    }

    /** Items dropped when the block is destroyed; default uses the loot table. */
    default List<ItemStackRef> drops(DropContext ctx) {
        return ctx.vanillaDrops();
    }
}
