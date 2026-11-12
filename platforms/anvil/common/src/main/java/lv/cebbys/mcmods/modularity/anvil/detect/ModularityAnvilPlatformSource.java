package lv.cebbys.mcmods.modularity.anvil.detect;

import lv.cebbys.mcmods.modularity.anvil.detect.ModularityAnvilLoaderApi.Family;
import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.api.core.PlatformVersion;
import lv.cebbys.mcmods.modularity.api.core.Side;
import lv.cebbys.mcmods.modularity.api.detect.DetectedPlatform;
import lv.cebbys.mcmods.modularity.api.detect.PlatformSource;

import java.util.Optional;

/**
 * Asks the Forge-family mod loader for its version, the side and the development flag. One source serves NeoForge and
 * Forge, so neither loader root repeats the other's code (docs/07, 7.3).
 */
public final class ModularityAnvilPlatformSource implements PlatformSource {
    private Family family;

    @Override
    public int priority() {
        return 1000;
    }

    @Override
    public String name() {
        return family().map(Family::sourceName).orElse("anvil-fml");
    }

    @Override
    public Optional<DetectedPlatform> detect() {
        Optional<Family> found = ModularityAnvilLoaderApi.present();
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Family family = found.get();
        this.family = family;
        Loader loader = family == Family.NEOFORGE ? Loader.NEOFORGE : Loader.FORGE;
        PlatformVersion version = ModularityAnvilLoaderApi.loaderVersion(family).map(PlatformVersion::parse)
                .orElse(PlatformVersion.unknown());
        Optional<Side> side = ModularityAnvilLoaderApi.client(family)
                .map(client -> client ? Side.CLIENT : Side.SERVER);
        return Optional.of(DetectedPlatform.of(loader, version, side, ModularityAnvilLoaderApi.development(family),
                family.sourceName()));
    }

    private Optional<Family> family() {
        return family == null ? ModularityAnvilLoaderApi.present() : Optional.of(family);
    }
}
