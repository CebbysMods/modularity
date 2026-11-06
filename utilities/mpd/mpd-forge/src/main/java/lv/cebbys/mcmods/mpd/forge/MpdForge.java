package lv.cebbys.mcmods.mpd.forge;

import lv.cebbys.mcmods.mpd.DetectedPlatform;
import lv.cebbys.mcmods.mpd.PlatformDetector;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Forge entrypoint of Minecraft Platform Detector: detects the platform once and logs it. */
@Mod("mpd")
public final class MpdForge {
    private static final Logger LOGGER = LoggerFactory.getLogger("mpd");

    public MpdForge() {
        DetectedPlatform platform = PlatformDetector.detect();
        LOGGER.info("[mpd] Detected platform {} (source: {})", platform, platform.source());
    }
}
