package lv.cebbys.mcmods.modularity.api.client;

import lv.cebbys.mcmods.modularity.api.Platform;
import lv.cebbys.mcmods.modularity.api.block.BlockHandle;

/** Everything a mod can do during client initialization. */
public interface ClientContext {
    String modId();

    Platform platform();

    /**
     * Renders {@code block} with a model computed per position. No block state or model JSON is
     * needed for that block. Works with Sodium/Iris through the platform renderer API.
     */
    void blockModel(BlockHandle block, DynamicModel model);

    /** Chooses which block state's texture the "block is being hit" particles use. */
    void hitParticles(BlockHandle block, HitParticles particles);
}
