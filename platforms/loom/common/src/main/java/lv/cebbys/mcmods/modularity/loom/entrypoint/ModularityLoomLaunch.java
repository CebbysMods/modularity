package lv.cebbys.mcmods.modularity.loom.entrypoint;

import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.api.detect.PlatformSource;
import lv.cebbys.mcmods.modularity.api.detect.VersionSource;
import lv.cebbys.mcmods.modularity.platform.EntryPoint;
import lv.cebbys.mcmods.modularity.platform.EntryPoints;
import lv.cebbys.mcmods.modularity.platform.ModularityLaunch;

/**
 * The launch stage of the Modularity lifecycle, shared by Fabric and Quilt Loader. It runs before the game starts.
 * The loader calls {@link #init} when it resolved the mods, and {@link #initMods} after the guest mods exist.
 */
public final class ModularityLoomLaunch {

    private ModularityLoomLaunch() {
    }

    /** The launch stage of every {@code "modularity"} entrypoint; the detection sources and the loader services only. */
    public static void init(Loader loader, PlatformSource platform, VersionSource version) {
        EntryPoints.active(EntryPoint.LAUNCH, loader);
        ModularityLoomServices.install(platform, version);
    }

    /** Runs the launch stage of every mod; the loader calls it after the guest mods exist. */
    public static void initMods(Loader loader) {
        ModularityLaunch.init(loader);
    }
}
