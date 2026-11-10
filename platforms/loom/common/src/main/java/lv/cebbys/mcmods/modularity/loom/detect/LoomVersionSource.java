package lv.cebbys.mcmods.modularity.loom.detect;

import lv.cebbys.mcmods.modularity.api.core.MinecraftVersion;
import lv.cebbys.mcmods.modularity.api.detect.VersionSource;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Optional;

/**
 * Version of the built-in {@code minecraft} mod of Fabric Loader or Quilt Loader. Only applies when
 * Fabric Loader's API is present.
 */
public final class LoomVersionSource implements VersionSource {
    @Override
    public int priority() {
        return 1000;
    }

    @Override
    public String name() {
        return "fabric-loader";
    }

    @Override
    public Optional<MinecraftVersion> detect() {
        if (!LoomClasses.exists(LoomClasses.FABRIC_LOADER)) {
            return Optional.empty();
        }
        return FabricLoader.getInstance().getModContainer("minecraft")
                .map(container -> MinecraftVersion.parse(
                        container.getMetadata().getVersion().getFriendlyString()));
    }
}
