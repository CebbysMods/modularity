package lv.cebbys.mcmods.modularity.anvil.entrypoint;

import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.platform.EntryPoint;
import lv.cebbys.mcmods.modularity.platform.EntryPoints;
import lv.cebbys.mcmods.modularity.platform.ModularityServer;

/** The dedicated server stage of the Modularity lifecycle on the Forge family. */
public final class ModularityAnvilServer {

    private ModularityAnvilServer() {
    }

    /** Runs the server stage of every Modularity entrypoint. */
    public static void init(Loader loader) {
        EntryPoints.active(EntryPoint.SERVER, loader);
        ModularityServer.init();
    }
}
