package lv.cebbys.mcmods.modularity.api.item;

import lv.cebbys.mcmods.modularity.api.core.InteractionResult;

/** Logic of an item you define. Defaults do nothing. */
public interface ItemBehaviour {
    ItemBehaviour NONE = new ItemBehaviour() {
    };

    /** The item is used on a block (both sides). */
    default InteractionResult useOnBlock(ItemUseContext ctx) {
        return InteractionResult.PASS;
    }
}
