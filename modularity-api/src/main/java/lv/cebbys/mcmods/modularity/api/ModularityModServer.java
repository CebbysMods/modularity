package lv.cebbys.mcmods.modularity.api;

import lv.cebbys.mcmods.modularity.api.server.ServerContext;

/**
 * The dedicated-server lifecycle of a Modularity mod. It runs on a dedicated server only, after
 * {@link ModularityModCommon#initialize(CommonContext)}, and never on the client, not even in
 * singleplayer.
 *
 * <p>A mod implements this interface only when it has server work; the method is default, so the
 * implementation may override nothing.</p>
 */
public interface ModularityModServer {
    /** Dedicated-server initialization. Never runs on the client (not even in singleplayer). */
    default void initialize(ServerContext server) {
    }
}
