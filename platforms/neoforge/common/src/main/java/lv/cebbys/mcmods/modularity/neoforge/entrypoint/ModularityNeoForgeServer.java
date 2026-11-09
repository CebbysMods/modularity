package lv.cebbys.mcmods.modularity.neoforge.entrypoint;

import lv.cebbys.mcmods.modularity.platform.EntryPoint;
import lv.cebbys.mcmods.modularity.platform.EntryPoints;
import lv.cebbys.mcmods.modularity.platform.ModularityServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;

/**
 * Dedicated server entrypoint of Modularity. NeoForge constructs this class on a dedicated server only, so the
 * client never loads server types. The init runs in {@code enqueueWork}, after the common init.
 */
@Mod(value = "modularity", dist = Dist.DEDICATED_SERVER)
public final class ModularityNeoForgeServer {

    public ModularityNeoForgeServer(IEventBus modBus) {
        modBus.addListener((FMLDedicatedServerSetupEvent event) -> event.enqueueWork(() -> {
            EntryPoints.active(EntryPoint.SERVER, "neoforge");
            ModularityServer.init();
        }));
    }
}
