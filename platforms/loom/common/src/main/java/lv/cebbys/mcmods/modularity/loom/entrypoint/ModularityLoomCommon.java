package lv.cebbys.mcmods.modularity.loom.entrypoint;

import lv.cebbys.mcmods.modularity.platform.EntryPoint;
import lv.cebbys.mcmods.modularity.platform.EntryPoints;
import lv.cebbys.mcmods.modularity.platform.ModularityCommon;
import lv.cebbys.mcmods.modularity.platform.compat.SyncedData;
import lv.cebbys.mcmods.modularity.platform.detect.DetectionLog;

/**
 * The common stage of the Modularity lifecycle, shared by Fabric and Quilt Loader. The loader entrypoint passes its
 * name for the log lines.
 */
public final class ModularityLoomCommon {

    private ModularityLoomCommon() {
    }

    /** Installs the loader services and initializes every {@code "modularity"} entrypoint. */
    public static void init(String loaderName) {
        EntryPoints.active(EntryPoint.COMMON, loaderName);
        ModularityLoomServices.install();
        SyncedData.init();
        DetectionLog.log();
        ModularityCommon.init(loaderName);
    }
}
