package lv.cebbys.mcmods.mvd.forge;

import lv.cebbys.mcmods.mvd.DetectedVersion;
import lv.cebbys.mcmods.mvd.VersionDetector;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Forge entrypoint of Minecraft Version Detector: detects the version once and logs it. */
@Mod("mvd")
public final class MvdForge {
    private static final Logger LOGGER = LoggerFactory.getLogger("mvd");

    public MvdForge() {
        DetectedVersion version = VersionDetector.detect();
        LOGGER.info("[mvd] Detected version {} (source: {})", version, version.source());
    }
}
