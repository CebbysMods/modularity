package lv.cebbys.mcmods.modularity.platform.detect;

import lv.cebbys.mcmods.modularity.api.core.MinecraftVersion;
import lv.cebbys.mcmods.modularity.api.detect.DetectedPlatform;
import lv.cebbys.mcmods.modularity.api.detect.PlatformDetector;
import lv.cebbys.mcmods.modularity.api.detect.VersionDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Logs the runtime detection once, after the loader installed its sources. */
public final class DetectionLog {
    private static final Logger LOGGER = LoggerFactory.getLogger("modularity");

    private DetectionLog() {
    }

    /** Writes {@code [modularity] platform <LOADER> <loaderVersion>, Minecraft <version>, <SIDE>}. */
    public static void log() {
        DetectedPlatform platform = PlatformDetector.detect();
        MinecraftVersion version = VersionDetector.detect();
        String side = platform.side().map(Object::toString).orElse("UNKNOWN");
        LOGGER.info("[modularity] platform {} {}, Minecraft {}, {}", platform.loader(), platform.loaderVersion(),
                version, side);
    }
}
