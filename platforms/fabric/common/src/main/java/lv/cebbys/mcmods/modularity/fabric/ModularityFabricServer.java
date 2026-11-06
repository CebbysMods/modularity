package lv.cebbys.mcmods.modularity.fabric;

import lv.cebbys.mcmods.modularity.api.Modularity;
import lv.cebbys.mcmods.modularity.api.Platform;
import lv.cebbys.mcmods.modularity.api.server.ServerContext;
import net.fabricmc.api.DedicatedServerModInitializer;

/** Fabric dedicated server entrypoint: runs {@code initServer} of every Modularity mod. */
public final class ModularityFabricServer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        ModularityCore.forEachMod("server initialization", loaded -> loaded.mod().initServer(new Context(loaded.modId())));
    }

    private record Context(String modId) implements ServerContext {
        @Override
        public Platform platform() {
            return Modularity.platform();
        }
    }
}
