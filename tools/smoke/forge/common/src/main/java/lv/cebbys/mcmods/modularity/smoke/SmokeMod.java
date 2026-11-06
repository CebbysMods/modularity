package lv.cebbys.mcmods.modularity.smoke;

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
 * Platform smoke test (docs/07-multi-version-build.md, 7.6), Forge: logs whether modularity, mvd and mpd are
 * loaded as separate top-level mods; on a dedicated server it stops the server after a few ticks.
 */
@Mod("modularity_smoke")
public final class SmokeMod {
    static final Logger LOGGER = LoggerFactory.getLogger("modularity-smoke");
    private static final int SERVER_TICKS = 40;

    private int ticks;

    public SmokeMod() {
        for (String id : new String[]{"modularity", "mvd", "mpd"}) {
            Optional<? extends ModContainer> container = ModList.get().getModContainerById(id);
            LOGGER.info("[smoke] mod {}: loaded={} version={} nested={}", id, container.isPresent(),
                    container.map(c -> c.getModInfo().getVersion().toString()).orElse("-"),
                    container.map(SmokeMod::nested).orElse("no"));
        }
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent.Post event) -> {
            if (event.getServer().isDedicatedServer() && ++ticks == SERVER_TICKS) {
                LOGGER.info("[smoke] server ticked {} times, stopping", SERVER_TICKS);
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
