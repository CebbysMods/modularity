package lv.cebbys.mcmods.modularity.anvil.entrypoint;

import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.platform.EntryPoint;
import lv.cebbys.mcmods.modularity.platform.EntryPoints;
import lv.cebbys.mcmods.modularity.platform.client.ModularityClient;
import lv.cebbys.mcmods.modularity.platform.loader.ClientHooks;
import lv.cebbys.mcmods.modularity.platform.loader.Loaders;
import org.jspecify.annotations.Nullable;

/**
 * The client stage of the Modularity lifecycle on the Forge family.
 *
 * <p>NeoForge and Forge post their client registration events (entity renderers, key mappings, reload listeners and,
 * on Forge, the HUD layers) while the game object is constructed. Their setup events fire after that, so a client
 * stage that waits for {@code FMLClientSetupEvent} adds its listeners too late and the game finds no renderer. The
 * client entrypoint therefore only prepares the client service, and the common stage runs the client stage at its end,
 * inside the first {@code RegisterEvent} pass (docs/07 7.8).</p>
 */
public final class ModularityAnvilClient {
    /** The client service the client entrypoint prepared, or {@code null} on a dedicated server. */
    private static @Nullable ClientHooks pending;

    private ModularityAnvilClient() {
    }

    /** Keeps the client service until the common stage runs the client stage. */
    public static void prepare(ClientHooks hooks) {
        pending = hooks;
    }

    /** Runs the prepared client stage, if this distribution has one, and forgets it. */
    public static void runPending(Loader loader) {
        ClientHooks hooks = pending;
        pending = null;
        if (hooks != null) {
            init(loader, hooks);
        }
    }

    /** Installs the client service and runs the client init. */
    public static void init(Loader loader, ClientHooks hooks) {
        EntryPoints.active(EntryPoint.CLIENT, loader);
        Loaders.installClient(hooks);
        ModularityClient.init();
    }
}
