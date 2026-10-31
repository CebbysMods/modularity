package lv.cebbys.mcmods.modularity.api.block;

import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.api.world.Level;
import lv.cebbys.mcmods.modularity.api.world.Player;

import java.util.List;
import java.util.Optional;

/**
 * Drops are being computed (server side) - player mining, explosions, commands. The block may
 * already be removed, but {@link #data} still returns the data it had.
 */
public interface DropContext extends BlockContext {
    Level level();

    /** The player that broke the block, empty for explosions and other causes. */
    Optional<Player> player();

    /** Tool used, empty stack when unknown. */
    ItemStackRef tool();

    /** Drops vanilla would produce from the loot table. */
    List<ItemStackRef> vanillaDrops();
}
