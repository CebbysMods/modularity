package lv.cebbys.mcmods.modularity.loom.entrypoint;

import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.platform.EntryPoint;
import lv.cebbys.mcmods.modularity.platform.EntryPoints;
import lv.cebbys.mcmods.modularity.platform.ModularityServer;

/** The dedicated server stage of the Modularity lifecycle, shared by Fabric and Quilt Loader. */
public final class ModularityLoomServer {

    private ModularityLoomServer() {
    }

    /** Runs the server stage of every {@code "modularity"} entrypoint. */
    public static void init(Loader loader) {
        EntryPoints.active(EntryPoint.SERVER, loader);
        ModularityServer.init();
    }
}
