package lv.cebbys.mcmods.modularity.api.client;

import lv.cebbys.mcmods.modularity.api.block.BlockStateRef;

/** Collects geometry for a {@link DynamicModel}. */
public interface ModelOutput {
    /** Renders the regular model of {@code state} at this position (including other mods' models). */
    void blockState(BlockStateRef state);
}
