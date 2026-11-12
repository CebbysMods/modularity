package lv.cebbys.mcmods.modularity.loom.entrypoint;

import lv.cebbys.mcmods.modularity.loom.client.ModularityLoomClientHooks;
import lv.cebbys.mcmods.modularity.platform.EntryPoint;
import lv.cebbys.mcmods.modularity.platform.EntryPoints;
import lv.cebbys.mcmods.modularity.platform.client.ModularityClient;
import lv.cebbys.mcmods.modularity.platform.loader.Loaders;

/** The client stage of the Modularity lifecycle, shared by Fabric and Quilt Loader. */
public final class ModularityLoomClient {

    private ModularityLoomClient() {
    }

    /** Installs the client service and runs the client init. */
    public static void init(String loaderName) {
        EntryPoints.active(EntryPoint.CLIENT, loaderName);
        Loaders.installClient(new ModularityLoomClientHooks());
        ModularityClient.init();
    }
}
