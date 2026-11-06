package lv.cebbys.mcmods.mpd.neoforge;

import lv.cebbys.mcmods.mpd.DetectedPlatform;
import lv.cebbys.mcmods.mpd.Platform;
import lv.cebbys.mcmods.mpd.PlatformSource;
import lv.cebbys.mcmods.mpd.Side;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;

import java.util.Optional;

/** Asks NeoForge's mod loader for its version, the side and the development flag. Only applies when NeoForge is present. */
public final class NeoForgePlatformSource implements PlatformSource {
    @Override
    public int priority() {
        return 1000;
    }

    @Override
    public String name() {
        return "neoforge-fml";
    }

    @Override
    public Optional<DetectedPlatform> detect() {
        try {
            Class.forName("net.neoforged.fml.loading.FMLLoader", false, NeoForgePlatformSource.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
        String version = FMLLoader.versionInfo().neoForgeVersion();
        Side side = FMLEnvironment.dist == Dist.CLIENT ? Side.CLIENT : Side.SERVER;
        return Optional.of(DetectedPlatform.of(Platform.NEOFORGE,
                version == null ? DetectedPlatform.UNKNOWN_VERSION : version, side, !FMLLoader.isProduction(), name()));
    }
}
