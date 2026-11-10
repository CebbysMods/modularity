package lv.cebbys.mcmods.modularity.smoke;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Platform smoke test (docs/07-multi-version-build.md, 7.6). Logs whether Modularity is loaded as a top-level mod;
 * on a dedicated server it stops the server after a few ticks. The plugin's {@code smokeServer} / {@code smokeClient}
 * tasks check these lines and the Modularity detection line.
 */
public final class SmokeMod implements ModInitializer {
    static final Logger LOGGER = LoggerFactory.getLogger("modularity-smoke");
    private static final int SERVER_TICKS = 40;
    private static final String GUEST = "smoke_guest";
    private static final String GUEST_PROPERTY = "modularity.smoke.guest";

    private int ticks;

    @Override
    public void onInitialize() {
        Optional<ModContainer> container = FabricLoader.getInstance().getModContainer("modularity");
        LOGGER.info("Mod modularity: loaded={} version={} nested={}", container.isPresent(),
                container.map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("-"),
                container.flatMap(ModContainer::getContainingMod).map(c -> c.getMetadata().getId()).orElse("no"));
        // Mods initialize in no fixed order: report the guest mod once everything has initialized.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> LOGGER.info("Guest mod {}: loaded={} initialized={}",
                GUEST, FabricLoader.getInstance().isModLoaded(GUEST), "initialized".equals(System.getProperty(GUEST_PROPERTY))));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.isDedicatedServer() && ++ticks == SERVER_TICKS) {
                LOGGER.info("Server ticked {} times, stopping", SERVER_TICKS);
                server.halt(false);
            }
        });
    }
}
