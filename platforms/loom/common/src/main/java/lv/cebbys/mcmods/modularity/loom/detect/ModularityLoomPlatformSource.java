package lv.cebbys.mcmods.modularity.loom.detect;

import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.api.core.PlatformVersion;
import lv.cebbys.mcmods.modularity.api.core.Side;
import lv.cebbys.mcmods.modularity.api.detect.DetectedPlatform;
import lv.cebbys.mcmods.modularity.api.detect.PlatformSource;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Optional;

/**
 * Asks Fabric Loader (or Quilt Loader, which provides the same API) for its version, the side and
 * the development flag. Reports {@link Loader#QUILT} when Quilt Loader is present.
 */
public final class ModularityLoomPlatformSource implements PlatformSource {
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
        if (!ModularityLoomClasses.exists(ModularityLoomClasses.FABRIC_LOADER)) {
            return Optional.empty();
        }
        FabricLoader loader = FabricLoader.getInstance();
        boolean quilt = ModularityLoomClasses.exists(ModularityLoomClasses.QUILT_LOADER);
        PlatformVersion version = loader.getModContainer(quilt ? "quilt_loader" : "fabricloader")
                .map(container -> PlatformVersion.parse(container.getMetadata().getVersion().getFriendlyString()))
                .orElse(PlatformVersion.unknown());
        Side side = loader.getEnvironmentType() == EnvType.CLIENT ? Side.CLIENT : Side.SERVER;
        return Optional.of(DetectedPlatform.of(quilt ? Loader.QUILT : Loader.FABRIC, version, side,
                loader.isDevelopmentEnvironment(), name()));
    }
}
