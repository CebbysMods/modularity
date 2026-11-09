package lv.cebbys.mcmods.modularity.fabric.entrypoint;

import lv.cebbys.mcmods.modularity.fabric.client.FabricClientHooks;
import lv.cebbys.mcmods.modularity.fabric.loader.FabricLoaderName;
import lv.cebbys.mcmods.modularity.platform.EntryPoint;
import lv.cebbys.mcmods.modularity.platform.EntryPoints;
import lv.cebbys.mcmods.modularity.platform.client.ModularityClient;
import lv.cebbys.mcmods.modularity.platform.loader.Loaders;
import net.fabricmc.api.ClientModInitializer;

/** Fabric client entrypoint: installs the Fabric client hooks and runs the client init. */
public final class ModularityFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntryPoints.active(EntryPoint.CLIENT, FabricLoaderName.get());
        Loaders.installClient(new FabricClientHooks());
        ModularityClient.init();
    }
}
