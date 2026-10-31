package lv.cebbys.mcmods.modularity.api;

import lv.cebbys.mcmods.modularity.api.client.ClientContext;

/**
 * The single entrypoint of a Modularity mod. The same class runs on every supported loader and
 * Minecraft version.
 *
 * <ul>
 *     <li>Fabric/Quilt: declare it as a {@code "modularity"} entrypoint in {@code fabric.mod.json}.</li>
 *     <li>NeoForge: (milestone 2) generated {@code @Mod} adapter.</li>
 * </ul>
 */
public interface ModularMod {
    /** Common initialization: register content, data, interceptors. Runs on client and server. */
    void init(ModContext mod);

    /** Client-only initialization: models, particles, renderers. Never runs on a dedicated server. */
    default void initClient(ClientContext client) {
    }
}
