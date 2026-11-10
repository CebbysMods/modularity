package lv.cebbys.mcmods.modularity.smoke;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforgespi.locating.IModFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Platform smoke test (docs/07-multi-version-build.md, 7.6), NeoForge: logs whether Modularity is loaded as a
 * top-level mod; on a dedicated server it stops the server after a few ticks.
 */
@Mod("modularity_smoke")
public final class SmokeMod {
    static final Logger LOGGER = LoggerFactory.getLogger("modularity-smoke");
    private static final int SERVER_TICKS = 40;

    private int ticks;

    public SmokeMod() {
        Optional<? extends ModContainer> container = ModList.get().getModContainerById("modularity");
        LOGGER.info("Mod modularity: loaded={} version={} nested={}", container.isPresent(),
                container.map(c -> c.getModInfo().getVersion().toString()).orElse("-"),
                container.map(SmokeMod::parent).orElse("no"));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
            if (event.getServer().isDedicatedServer() && ++ticks == SERVER_TICKS) {
                LOGGER.info("Server ticked {} times, stopping", SERVER_TICKS);
                event.getServer().halt(false);
            }
        });
    }

    /** Id of the mod whose jar contains this mod's jar, or {@code no}. */
    private static String parent(ModContainer container) {
        IModFile parent = container.getModInfo().getOwningFile().getFile().getDiscoveryAttributes().parent();
        if (parent == null) {
            return "no";
        }
        return parent.getModInfos().isEmpty() ? parent.getFileName() : parent.getModInfos().get(0).getModId();
    }
}
