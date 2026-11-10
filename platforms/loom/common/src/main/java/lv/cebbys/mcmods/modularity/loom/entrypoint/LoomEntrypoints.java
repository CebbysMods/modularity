package lv.cebbys.mcmods.modularity.loom.entrypoint;

import lv.cebbys.mcmods.modularity.api.detect.PlatformDetector;
import lv.cebbys.mcmods.modularity.api.detect.VersionDetector;
import lv.cebbys.mcmods.modularity.loom.client.LoomClientHooks;
import lv.cebbys.mcmods.modularity.loom.detect.LoomPlatformSource;
import lv.cebbys.mcmods.modularity.loom.detect.LoomVersionSource;
import lv.cebbys.mcmods.modularity.loom.loader.LoomLoaderHooks;
import lv.cebbys.mcmods.modularity.loom.loader.LoomLoaderInfo;
import lv.cebbys.mcmods.modularity.loom.loader.LoomRenderData;
import lv.cebbys.mcmods.modularity.loom.loader.LoomStorageBackend;
import lv.cebbys.mcmods.modularity.platform.EntryPoint;
import lv.cebbys.mcmods.modularity.platform.EntryPoints;
import lv.cebbys.mcmods.modularity.platform.ModularityCommon;
import lv.cebbys.mcmods.modularity.platform.ModularityLaunch;
import lv.cebbys.mcmods.modularity.platform.ModularityServer;
import lv.cebbys.mcmods.modularity.platform.client.ModularityClient;
import lv.cebbys.mcmods.modularity.platform.compat.SyncedData;
import lv.cebbys.mcmods.modularity.platform.detect.DetectionLog;
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
        installDetection();
        SyncedData.init();
        DetectionLog.log();
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

    /** Runs before the game starts, after the loader resolved the mods: detection and the seams only. */
    public static void launch(String loaderName) {
        EntryPoints.active(EntryPoint.LAUNCH, loaderName);
        installDetection();
    }

    /** Runs the launch stage of every mod; the loader calls it after the guest mods exist. */
    public static void launchMods(String loaderName) {
        ModularityLaunch.init(loaderName);
    }

    /** Installs the detection sources and the seams; a second call is harmless. */
    private static void installDetection() {
        VersionDetector.install(new LoomVersionSource());
        PlatformDetector.install(new LoomPlatformSource());
        Loaders.install(new LoomLoaderInfo(), new LoomRenderData(), new LoomStorageBackend(), new LoomLoaderHooks());
    }
}
