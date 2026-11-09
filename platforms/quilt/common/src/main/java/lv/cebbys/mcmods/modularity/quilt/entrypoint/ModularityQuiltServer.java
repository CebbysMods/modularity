package lv.cebbys.mcmods.modularity.quilt.entrypoint;

import lv.cebbys.mcmods.modularity.loom.entrypoint.LoomEntrypoints;
import net.fabricmc.api.DedicatedServerModInitializer;

/** Quilt dedicated server entrypoint: runs {@code initServer} of every Modularity mod. */
public final class ModularityQuiltServer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        LoomEntrypoints.server("quilt");
    }
}
