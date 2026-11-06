package lv.cebbys.mcmods.modularity.fabric.client;

import lv.cebbys.mcmods.modularity.fabric.loader.Loaders;
import net.fabricmc.api.ClientModInitializer;

/** Fabric client entrypoint: installs the Fabric client hooks and runs the client init. */
public final class ModularityFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Loaders.installClient(new FabricClientHooks());
        ModularityClient.init();
    }
}
