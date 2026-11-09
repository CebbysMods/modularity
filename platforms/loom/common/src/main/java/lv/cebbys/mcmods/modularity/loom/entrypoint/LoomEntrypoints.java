package lv.cebbys.mcmods.modularity.loom.entrypoint;

import lv.cebbys.mcmods.modularity.loom.client.LoomClientHooks;
import lv.cebbys.mcmods.modularity.loom.loader.LoomLoaderHooks;
import lv.cebbys.mcmods.modularity.loom.loader.LoomLoaderInfo;
import lv.cebbys.mcmods.modularity.loom.loader.LoomRenderData;
import lv.cebbys.mcmods.modularity.loom.loader.LoomStorageBackend;
import lv.cebbys.mcmods.modularity.platform.EntryPoint;
import lv.cebbys.mcmods.modularity.platform.EntryPoints;
import lv.cebbys.mcmods.modularity.platform.ModularityCommon;
import lv.cebbys.mcmods.modularity.platform.ModularityServer;
import lv.cebbys.mcmods.modularity.platform.client.ModularityClient;
import lv.cebbys.mcmods.modularity.platform.compat.SyncedData;
import lv.cebbys.mcmods.modularity.platform.loader.Loaders;

/**
 * The Modularity lifecycle behind the Fabric-compatibility entrypoints, shared by Fabric and Quilt Loader. The loader
 * entrypoint passes its name for the log line; the seams it installs are the same on both loaders.
 */
public final class LoomEntrypoints {

    private LoomEntrypoints() {
    }

    /** Installs the seams and initializes every {@code "modularity"} entrypoint. */
    public static void common(String loaderName) {
        EntryPoints.active(EntryPoint.COMMON, loaderName);
        Loaders.install(new LoomLoaderInfo(), new LoomRenderData(), new LoomStorageBackend(), new LoomLoaderHooks());
        SyncedData.init();
        ModularityCommon.init(loaderName);
    }

    public static void client(String loaderName) {
        EntryPoints.active(EntryPoint.CLIENT, loaderName);
        Loaders.installClient(new LoomClientHooks());
        ModularityClient.init();
    }

    public static void server(String loaderName) {
        EntryPoints.active(EntryPoint.SERVER, loaderName);
        ModularityServer.init();
    }

    /** Runs before the game starts, after the loader resolved the mods. */
    public static void launch(String loaderName) {
        EntryPoints.active(EntryPoint.LAUNCH, loaderName);
    }
}
