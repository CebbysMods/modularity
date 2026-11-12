package lv.cebbys.mcmods.modularity.platform;

import lv.cebbys.mcmods.modularity.api.Modularity;
import lv.cebbys.mcmods.modularity.api.ModularityModCommon;
import lv.cebbys.mcmods.modularity.api.ModularityModServer;
import lv.cebbys.mcmods.modularity.api.Platform;
import lv.cebbys.mcmods.modularity.api.server.ServerContext;

/**
 * Runs the server stage of every loaded mod built on Modularity. A loader calls it on a dedicated server only,
 * after common initialization. A mod without the stage is skipped.
 */
public final class ModularityServer {
    private ModularityServer() {
    }

    public static void init() {
        ModularityCommon.forEachMod("server initialization",
                loaded -> run(loaded.mod(), new Context(loaded.modId())));
    }

    private static void run(ModularityModCommon mod, ServerContext context) {
        if (mod instanceof ModularityModServer server) {
            server.initialize(context);
        }
    }

    private record Context(String modId) implements ServerContext {
        @Override
        public Platform platform() {
            return Modularity.platform();
        }
    }
}
