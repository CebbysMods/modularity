package lv.cebbys.mcmods.mvd.neoforge;

import lv.cebbys.mcmods.mvd.DetectedVersion;
import lv.cebbys.mcmods.mvd.VersionDetector;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** NeoForge entrypoint of Minecraft Version Detector: detects the version once and logs it. */
@Mod("mvd")
public final class MvdNeoForge {
    private static final Logger LOGGER = LoggerFactory.getLogger("mvd");

    public MvdNeoForge() {
        DetectedVersion version = VersionDetector.detect();
        LOGGER.info("[mvd] Detected version {} (source: {})", version, version.source());
    }
}
