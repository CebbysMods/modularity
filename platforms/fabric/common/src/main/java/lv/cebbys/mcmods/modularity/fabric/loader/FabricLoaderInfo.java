package lv.cebbys.mcmods.modularity.fabric.loader;

import lv.cebbys.mcmods.modularity.api.ModularMod;
import lv.cebbys.mcmods.modularity.api.core.Side;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;
import java.util.Optional;

/** {@link LoaderInfo} from Fabric Loader (also Quilt Loader, which provides the same API). */
public final class FabricLoaderInfo implements LoaderInfo {
    public static final String ENTRYPOINT = "modularity";

    @Override
    public Side side() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ? Side.CLIENT : Side.SERVER;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public Optional<String> modVersion(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getVersion().getFriendlyString());
    }

    @Override
    public boolean isDevelopment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public List<ModEntry> modularMods() {
        return FabricLoader.getInstance().getEntrypointContainers(ENTRYPOINT, ModularMod.class).stream()
                .map(c -> new ModEntry(c.getProvider().getMetadata().getId(), c::getEntrypoint))
                .toList();
    }
}
