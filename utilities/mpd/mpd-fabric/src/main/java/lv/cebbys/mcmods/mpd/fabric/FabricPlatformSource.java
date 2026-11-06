package lv.cebbys.mcmods.mpd.fabric;

import lv.cebbys.mcmods.mpd.DetectedPlatform;
import lv.cebbys.mcmods.mpd.Platform;
import lv.cebbys.mcmods.mpd.PlatformSource;
import lv.cebbys.mcmods.mpd.Side;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Optional;

/**
 * Asks Fabric Loader (or Quilt Loader, which provides the same API) for its version, the side and
 * the development flag. Only applies when Fabric Loader's API is present.
 */
public final class FabricPlatformSource implements PlatformSource {
    @Override
    public int priority() {
        return 1000;
    }

    @Override
    public String name() {
        return "fabric-loader";
    }

    @Override
    public Optional<DetectedPlatform> detect() {
        if (!exists("net.fabricmc.loader.api.FabricLoader")) {
            return Optional.empty();
        }
        FabricLoader loader = FabricLoader.getInstance();
        boolean quilt = exists("org.quiltmc.loader.api.QuiltLoader");
        String version = loader.getModContainer(quilt ? "quilt_loader" : "fabricloader")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse(DetectedPlatform.UNKNOWN_VERSION);
        Side side = loader.getEnvironmentType() == EnvType.CLIENT ? Side.CLIENT : Side.SERVER;
        return Optional.of(DetectedPlatform.of(quilt ? Platform.QUILT : Platform.FABRIC, version, side,
                loader.isDevelopmentEnvironment(), name()));
    }

    private static boolean exists(String className) {
        try {
            Class.forName(className, false, FabricPlatformSource.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}
