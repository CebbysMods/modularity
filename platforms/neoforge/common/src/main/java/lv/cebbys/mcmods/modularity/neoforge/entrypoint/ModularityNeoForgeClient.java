package lv.cebbys.mcmods.modularity.neoforge.entrypoint;

import lv.cebbys.mcmods.modularity.neoforge.client.NeoForgeClientHooks;
import lv.cebbys.mcmods.modularity.platform.EntryPoint;
import lv.cebbys.mcmods.modularity.platform.EntryPoints;
import lv.cebbys.mcmods.modularity.platform.client.ModularityClient;
import lv.cebbys.mcmods.modularity.platform.loader.Loaders;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client entrypoint of Modularity. NeoForge constructs this class on the client only, so the dedicated server
 * never loads client types. The init runs in {@code enqueueWork}, after the common init.
 */
@Mod(value = "modularity", dist = Dist.CLIENT)
public final class ModularityNeoForgeClient {

    public ModularityNeoForgeClient(IEventBus modBus) {
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> {
            EntryPoints.active(EntryPoint.CLIENT, "neoforge");
            Loaders.installClient(new NeoForgeClientHooks(modBus));
            ModularityClient.init();
        }));
    }
}
