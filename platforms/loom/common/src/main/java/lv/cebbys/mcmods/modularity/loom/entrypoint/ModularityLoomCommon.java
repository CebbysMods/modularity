package lv.cebbys.mcmods.modularity.loom.entrypoint;

import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.api.detect.PlatformSource;
import lv.cebbys.mcmods.modularity.api.detect.VersionSource;
import lv.cebbys.mcmods.modularity.platform.EntryPoint;
import lv.cebbys.mcmods.modularity.platform.EntryPoints;
import lv.cebbys.mcmods.modularity.platform.ModularityCommon;
import lv.cebbys.mcmods.modularity.platform.compat.SyncedData;
import lv.cebbys.mcmods.modularity.platform.detect.DetectionLog;

/**
 * The common stage of the Modularity lifecycle, shared by Fabric and Quilt Loader. The loader entrypoint passes its
 * loader and its detection sources for the log lines and the report.
 */
public final class ModularityLoomCommon {

    private ModularityLoomCommon() {
    }

    /** Installs the loader services and initializes every {@code "modularity"} entrypoint. */
    public static void init(Loader loader, PlatformSource platform, VersionSource version) {
        EntryPoints.active(EntryPoint.COMMON, loader);
        ModularityLoomServices.install(platform, version);
        SyncedData.init();
        DetectionLog.log();
        ModularityCommon.init(loader);
    }
}
