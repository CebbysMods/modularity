package lv.cebbys.mcmods.modularity.forge.entrypoint;

import lv.cebbys.mcmods.modularity.anvil.entrypoint.*;
import lv.cebbys.mcmods.modularity.forge.client.ForgeClientHooks;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Client entrypoint of Modularity. {@code ModularityForge} registers it only on the client, so the dedicated server
 * never loads client types. It prepares the client service, and the common stage runs the client stage at its end:
 * Forge posts its client registration events during the game construction, before the setup events, so a stage that
 * waited for {@code FMLClientSetupEvent} would add its listeners too late (docs/07 7.8).
 */
public final class ModularityForgeClient {

    private ModularityForgeClient() {
    }

    public static void register(IEventBus modBus) {
        ModularityAnvilClient.prepare(new ForgeClientHooks(modBus));
    }
}
