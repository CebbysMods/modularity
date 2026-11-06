package lv.cebbys.mcmods.modularity.neoforge;

import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NeoForge entrypoint of Modularity. The platform runtime (running the {@code ModularMod}s, content registration)
 * follows; so far the platform loads with mvd and mpd as separate mods.
 */
@Mod("modularity")
public final class ModularityNeoForge {
    private static final Logger LOGGER = LoggerFactory.getLogger("modularity");

    public ModularityNeoForge() {
        LOGGER.info("[modularity] NeoForge platform loaded");
    }
}
