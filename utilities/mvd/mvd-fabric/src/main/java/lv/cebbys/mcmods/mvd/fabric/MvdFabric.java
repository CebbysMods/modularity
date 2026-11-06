package lv.cebbys.mcmods.mvd.fabric;

import lv.cebbys.mcmods.mvd.DetectedVersion;
import lv.cebbys.mcmods.mvd.VersionDetector;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Fabric entrypoint of Minecraft Version Detector: detects the version once and logs it. */
public final class MvdFabric implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("mvd");

    @Override
    public void onInitialize() {
        DetectedVersion version = VersionDetector.detect();
        LOGGER.info("[mvd] Detected version {} (source: {})", version, version.source());
    }
}
