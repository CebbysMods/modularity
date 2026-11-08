package lv.cebbys.mcmods.modularity.api.block;

import lv.cebbys.mcmods.modularity.api.world.Level;
import lv.cebbys.mcmods.modularity.api.world.Player;

/** A player is about to break the block (called on both sides, before removal). */
public interface BreakContext extends BlockContext {
    Level level();

    Player player();

    /**
     * Plays what vanilla plays when {@code state} is broken: break particles and sound, plus vanilla
     * reactions such as angering piglins for {@link BlockTag#GUARDED_BY_PIGLINS}. Call it once per
     * state when a block visually consists of several blocks.
     */
    void breakEffects(BlockStateRef state);
}
