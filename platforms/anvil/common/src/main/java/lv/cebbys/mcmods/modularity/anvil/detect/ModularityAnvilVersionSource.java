package lv.cebbys.mcmods.modularity.anvil.detect;

import lv.cebbys.mcmods.modularity.anvil.detect.ModularityAnvilLoaderApi.Family;
import lv.cebbys.mcmods.modularity.api.core.MinecraftVersion;
import lv.cebbys.mcmods.modularity.api.detect.VersionSource;

import java.util.Optional;

/** Minecraft version as reported by the Forge-family mod loader; one source for NeoForge and Forge. */
public final class ModularityAnvilVersionSource implements VersionSource {
    @Override
    public int priority() {
        return 1000;
    }

    @Override
    public String name() {
        return ModularityAnvilLoaderApi.present().map(Family::sourceName).orElse("anvil-fml");
    }

    @Override
    public Optional<MinecraftVersion> detect() {
        return ModularityAnvilLoaderApi.present()
                .flatMap(ModularityAnvilLoaderApi::minecraftVersion)
                .map(MinecraftVersion::parse);
    }
}
