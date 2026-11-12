package lv.cebbys.mcmods.modularity.anvil.entrypoint;

import lv.cebbys.mcmods.modularity.anvil.ModularityAnvilServices;
import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.platform.EntryPoint;
import lv.cebbys.mcmods.modularity.platform.EntryPoints;
import lv.cebbys.mcmods.modularity.platform.ModularityCommon;
import lv.cebbys.mcmods.modularity.platform.compat.SyncedData;
import lv.cebbys.mcmods.modularity.platform.detect.DetectionLog;

/**
 * The common stage of the Modularity lifecycle on the Forge family. Both loaders call it on the first
 * {@code RegisterEvent}: Forge and NeoForge freeze the vanilla registries at the end of mod construction and unfreeze
 * them for that pass only, so an eagerly constructed value, such as a {@code Block}, fails on {@code NewRegistryEvent}
 * with "Registry is already frozen".
 */
public final class ModularityAnvilCommon {

    private ModularityAnvilCommon() {
    }

    /** Installs the shared services and initializes every Modularity entrypoint. */
    public static void init(Loader loader, ModularityAnvilServices services) {
        EntryPoints.active(EntryPoint.COMMON, loader);
        services.install();
        SyncedData.init();
        DetectionLog.log();
        ModularityCommon.init(loader);
    }
}
