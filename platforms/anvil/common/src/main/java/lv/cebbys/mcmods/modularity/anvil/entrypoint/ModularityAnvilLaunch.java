package lv.cebbys.mcmods.modularity.anvil.entrypoint;

import lv.cebbys.mcmods.modularity.anvil.ModularityAnvilServices;
import lv.cebbys.mcmods.modularity.platform.EntryPoint;
import lv.cebbys.mcmods.modularity.platform.EntryPoints;
import lv.cebbys.mcmods.modularity.platform.ModularityLaunch;

/**
 * The launch stage of the Modularity lifecycle on the Forge family. It runs in the mod constructor, before the game
 * starts. The loader calls {@link #init} with its services, and {@link #initMods} after the guest mods exist.
 */
public final class ModularityAnvilLaunch {

    private ModularityAnvilLaunch() {
    }

    /** The launch stage of every Modularity entrypoint, and the shared services. */
    public static void init(String loaderName, ModularityAnvilServices services) {
        EntryPoints.active(EntryPoint.LAUNCH, loaderName);
        services.install();
    }

    /** Runs the launch stage of every mod. Both loaders call it on the first {@code RegisterEvent}. */
    public static void initMods(String loaderName) {
        ModularityLaunch.init(loaderName);
    }
}
