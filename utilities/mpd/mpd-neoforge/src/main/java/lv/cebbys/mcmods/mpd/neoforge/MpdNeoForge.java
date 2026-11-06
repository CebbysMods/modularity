package lv.cebbys.mcmods.mpd.neoforge;

import lv.cebbys.mcmods.mpd.DetectedPlatform;
import lv.cebbys.mcmods.mpd.PlatformDetector;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** NeoForge entrypoint of Minecraft Platform Detector: detects the platform once and logs it. */
@Mod("mpd")
public final class MpdNeoForge {
    private static final Logger LOGGER = LoggerFactory.getLogger("mpd");

    public MpdNeoForge() {
        DetectedPlatform platform = PlatformDetector.detect();
        LOGGER.info("[mpd] Detected platform {} (source: {})", platform, platform.source());
    }
}
