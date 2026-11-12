package lv.cebbys.mcmods.modularity.forge.entrypoint;

import lv.cebbys.mcmods.modularity.anvil.entrypoint.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;

/**
 * Dedicated server entrypoint of Modularity. {@code ModularityForge} registers it only on a dedicated server, so the
 * client never loads server types. The init runs in {@code enqueueWork}, after the common init.
 */
public final class ModularityForgeServer {

    private ModularityForgeServer() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((FMLDedicatedServerSetupEvent event) -> event.enqueueWork(() ->
                ModularityAnvilServer.init("forge")));
    }
}
