package lv.cebbys.mcmods.modularity.fabric;

import lv.cebbys.mcmods.modularity.api.Modularity;
import lv.cebbys.mcmods.modularity.api.Platform;
import lv.cebbys.mcmods.modularity.api.server.ServerContext;
import net.fabricmc.api.DedicatedServerModInitializer;

/** Fabric dedicated server entrypoint: runs {@code initServer} of every Modularity mod. */
public final class ModularityFabricServer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        for (var loaded : ModularityFabric.mods()) {
            try {
                loaded.mod().initServer(new Context(loaded.modId()));
            } catch (RuntimeException e) {
                throw new IllegalStateException("Modularity mod '" + loaded.modId() + "' failed server initialization", e);
            }
        }
    }

    private record Context(String modId) implements ServerContext {
        @Override
        public Platform platform() {
            return Modularity.platform();
        }
    }
}
