package lv.cebbys.mcmods.modularity.fabric.entrypoint;

import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.loom.entrypoint.ModularityLoomServer;
import net.fabricmc.api.DedicatedServerModInitializer;

/** Fabric dedicated server entrypoint: runs the server stage of every Modularity mod. */
public final class ModularityFabricServer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        ModularityLoomServer.init(Loader.FABRIC);
    }
}
