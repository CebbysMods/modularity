package lv.cebbys.mcmods.modularity.api;

import lv.cebbys.mcmods.modularity.api.client.ClientContext;
import lv.cebbys.mcmods.modularity.api.server.ServerContext;

/**
 * The single entrypoint of a Modularity mod. The same class runs on every supported loader and
 * Minecraft version, and the same instance receives all three calls.
 *
 * <ol>
 *     <li>{@link #initCommon} - always, first.</li>
 *     <li>{@link #initClient} - only on the physical client.</li>
 *     <li>{@link #initServer} - only on a dedicated server.</li>
 * </ol>
 *
 * <ul>
 *     <li>Fabric/Quilt: declare it as a {@code "modularity"} entrypoint in {@code fabric.mod.json}.</li>
 *     <li>NeoForge: (milestone 2) generated {@code @Mod} adapter.</li>
 * </ul>
 */
public interface ModularMod {
    /** Common initialization: register content, data, interceptors. Runs on client and server. */
    void initCommon(ModContext mod);

    /** Client-only initialization: models, particles, renderers. Never runs on a dedicated server. */
    default void initClient(ClientContext client) {
    }

    /** Dedicated-server-only initialization. Never runs on the client (not even in singleplayer). */
    default void initServer(ServerContext server) {
    }
}
