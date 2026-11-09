package lv.cebbys.mcmods.modularity.platform;

import lv.cebbys.mcmods.modularity.api.Modularity;
import lv.cebbys.mcmods.modularity.api.Platform;
import lv.cebbys.mcmods.modularity.api.server.ServerContext;

/**
 * Runs {@code initServer} of every loaded mod built on Modularity. A loader calls it on a dedicated server only,
 * after common initialization.
 */
public final class ModularityServer {
    private ModularityServer() {
    }

    public static void init() {
        ModularityCore.forEachMod("server initialization",
                loaded -> loaded.mod().initServer(new Context(loaded.modId())));
    }

    private record Context(String modId) implements ServerContext {
        @Override
        public Platform platform() {
            return Modularity.platform();
        }
    }
}
