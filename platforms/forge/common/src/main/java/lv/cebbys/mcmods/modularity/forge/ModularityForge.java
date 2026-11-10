package lv.cebbys.mcmods.modularity.forge;

import lv.cebbys.mcmods.modularity.api.detect.PlatformDetector;
import lv.cebbys.mcmods.modularity.api.detect.VersionDetector;
import lv.cebbys.mcmods.modularity.forge.detect.ForgePlatformSource;
import lv.cebbys.mcmods.modularity.forge.detect.ForgeVersionSource;
import lv.cebbys.mcmods.modularity.platform.EntryPoints;
import lv.cebbys.mcmods.modularity.platform.detect.DetectionLog;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forge entrypoint of Modularity. The constructor installs the detection sources and logs the result; the rest of
 * the platform runtime (running the {@code ModularMod}s, content registration) follows.
 */
@Mod("modularity")
public final class ModularityForge {
    private static final Logger LOGGER = LoggerFactory.getLogger("modularity");

    public ModularityForge() {
        VersionDetector.install(new ForgeVersionSource());
        PlatformDetector.install(new ForgePlatformSource());
        EntryPoints.active("mod-constructor", "forge");
        DetectionLog.log();
        LOGGER.info("[modularity] Forge platform loaded");
    }
}
