package lv.cebbys.mcmods.modularity.forge.detect;

import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.api.core.PlatformVersion;
import lv.cebbys.mcmods.modularity.api.core.Side;
import lv.cebbys.mcmods.modularity.api.detect.DetectedPlatform;
import lv.cebbys.mcmods.modularity.api.detect.PlatformSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;

import java.util.Optional;

/** Asks Forge's mod loader for its version, the side and the development flag. Only applies when Forge is present. */
public final class ForgePlatformSource implements PlatformSource {
    @Override
    public int priority() {
        return 1000;
    }

    @Override
    public String name() {
        return "forge-fml";
    }

    @Override
    public Optional<DetectedPlatform> detect() {
        try {
            Class.forName("net.minecraftforge.fml.loading.FMLLoader", false,
                    ForgePlatformSource.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
        String version = FMLLoader.versionInfo().forgeVersion();
        Side side = FMLEnvironment.dist == Dist.CLIENT ? Side.CLIENT : Side.SERVER;
        return Optional.of(DetectedPlatform.of(Loader.FORGE,
                version == null ? PlatformVersion.unknown() : PlatformVersion.parse(version), side,
                !FMLLoader.isProduction(), name()));
    }
}
