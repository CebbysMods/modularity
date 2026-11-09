package lv.cebbys.mcmods.modularity.quilt.entrypoint;

import lv.cebbys.mcmods.modularity.loom.entrypoint.LoomEntrypoints;
import net.fabricmc.api.ClientModInitializer;

/** Quilt client entrypoint: installs the Loom client hooks and runs the client init. */
public final class ModularityQuiltClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LoomEntrypoints.client("quilt");
    }
}
