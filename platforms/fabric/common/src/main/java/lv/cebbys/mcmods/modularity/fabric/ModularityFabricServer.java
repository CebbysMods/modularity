package lv.cebbys.mcmods.modularity.fabric;

import lv.cebbys.mcmods.modularity.fabric.loader.FabricLoaderName;
import lv.cebbys.mcmods.modularity.platform.EntryPoints;
import lv.cebbys.mcmods.modularity.platform.ModularityServer;
import net.fabricmc.api.DedicatedServerModInitializer;

/** Fabric dedicated server entrypoint: runs {@code initServer} of every Modularity mod. */
public final class ModularityFabricServer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        EntryPoints.active("server", FabricLoaderName.get());
        ModularityServer.init();
    }
}
