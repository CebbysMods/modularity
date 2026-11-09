package lv.cebbys.mcmods.modularity.forge;

import lv.cebbys.mcmods.modularity.platform.EntryPoints;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forge entrypoint of Modularity. The platform runtime (running the {@code ModularMod}s, content registration)
 * follows; so far the platform loads with mvd and mpd as separate mods.
 */
@Mod("modularity")
public final class ModularityForge {
    private static final Logger LOGGER = LoggerFactory.getLogger("modularity");

    public ModularityForge() {
        EntryPoints.active("mod-constructor", "forge");
        LOGGER.info("[modularity] Forge platform loaded");
    }
}
