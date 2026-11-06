package lv.cebbys.mcmods.mpd.fabric;

import lv.cebbys.mcmods.mpd.DetectedPlatform;
import lv.cebbys.mcmods.mpd.PlatformDetector;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Fabric entrypoint of Minecraft Platform Detector: detects the platform once and logs it. */
public final class MpdFabric implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("mpd");

    @Override
    public void onInitialize() {
        DetectedPlatform platform = PlatformDetector.detect();
        LOGGER.info("[mpd] Detected platform {} (source: {})", platform, platform.source());
    }
}
