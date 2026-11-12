package lv.cebbys.mcmods.modularity.neoforge.entrypoint;

import lv.cebbys.mcmods.modularity.anvil.entrypoint.*;
import lv.cebbys.mcmods.modularity.neoforge.client.NeoForgeClientHooks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Client entrypoint of Modularity. NeoForge constructs this class on the client only, so the dedicated server never
 * loads client types. It prepares the client service, and the common stage runs the client stage at its end: NeoForge
 * posts its client registration events during the game construction, before the setup events, so a stage that waited
 * for {@code FMLClientSetupEvent} would add its listeners too late (docs/07 7.8).
 */
@Mod(value = "modularity", dist = Dist.CLIENT)
public final class ModularityNeoForgeClient {

    public ModularityNeoForgeClient(IEventBus modBus) {
        ModularityAnvilClient.prepare(new NeoForgeClientHooks(modBus));
    }
}
