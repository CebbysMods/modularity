package lv.cebbys.mcmods.modularity.smoke;

import lv.cebbys.mcmods.modularity.api.Modularity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Platform smoke test (docs/07-multi-version-build.md, 7.6), Forge: logs whether Modularity is loaded as a top-level
 * mod; on a dedicated server it stops the server after a few ticks.
 */
@Mod("modularity_smoke")
public final class SmokeMod {
    static final Logger LOGGER = LoggerFactory.getLogger("modularity-smoke");
    private static final int SERVER_TICKS = 40;

    private int ticks;

    public SmokeMod() {
        // The smoke mod is a Modularity mod on every loader. Modularity runs its common stage on NewRegistryEvent,
        // which fires after this constructor, so the registration is accepted (bean modularity-4uol).
        Modularity.registerMod(SmokeFeatures.MOD_ID, new SmokeFeatures());
        Optional<? extends ModContainer> container = ModList.get().getModContainerById("modularity");
        LOGGER.info("Mod modularity: loaded={} version={} nested={}", container.isPresent(),
                container.map(c -> c.getModInfo().getVersion().toString()).orElse("-"),
                container.map(SmokeMod::nested).orElse("no"));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent.Post event) -> {
            if (event.getServer().isDedicatedServer() && ++ticks == SERVER_TICKS) {
                LOGGER.info("Server ticked {} times, stopping", SERVER_TICKS);
                event.getServer().halt(false);
            }
        });
        if (FMLEnvironment.dist.isClient()) {
            SmokeClient.install();
        }
    }

    /**
     * Forge's mod file API names no parent file; a jar-in-jar mod comes from inside {@code META-INF/jarjar} of its
     * parent jar.
     */
    private static String nested(ModContainer container) {
        String path = container.getModInfo().getOwningFile().getFile().getFilePath().toString().replace('\\', '/');
        return path.contains("META-INF/jarjar/") ? "jarjar" : "no";
    }
}
