package lv.cebbys.mcmods.modularity.fabric.entrypoint;

import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.loom.entrypoint.ModularityLoomClient;
import net.fabricmc.api.ClientModInitializer;

/** Fabric client entrypoint: installs the Fabric client hooks and runs the client init. */
public final class ModularityFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModularityLoomClient.init(Loader.FABRIC);
    }
}
