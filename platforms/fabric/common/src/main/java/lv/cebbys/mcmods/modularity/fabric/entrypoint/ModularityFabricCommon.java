package lv.cebbys.mcmods.modularity.fabric.entrypoint;

import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.fabric.detect.ModularityFabricPlatformSource;
import lv.cebbys.mcmods.modularity.fabric.detect.ModularityFabricVersionSource;
import lv.cebbys.mcmods.modularity.loom.entrypoint.ModularityLoomCommon;
import net.fabricmc.api.ModInitializer;

/** Fabric common entrypoint: installs the Fabric services and initializes every {@code "modularity"} entrypoint. */
public final class ModularityFabricCommon implements ModInitializer {
    @Override
    public void onInitialize() {
        ModularityLoomCommon.init(Loader.FABRIC, new ModularityFabricPlatformSource(), new ModularityFabricVersionSource());
    }
}
