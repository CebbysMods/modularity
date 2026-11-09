package lv.cebbys.mcmods.modularity.neoforge.loader;

import lv.cebbys.mcmods.modularity.api.ModularModRegistry;
import lv.cebbys.mcmods.modularity.api.core.Side;
import lv.cebbys.mcmods.modularity.platform.loader.LoaderInfo;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.List;
import java.util.Optional;

/**
 * {@link LoaderInfo} from NeoForge. Every {@link ModList} call is lazy: the list exists only from common setup
 * onward, and the constructor installs this object before that.
 */
public final class NeoForgeLoaderInfo implements LoaderInfo {

    @Override
    public Side side() {
        return FMLEnvironment.dist == Dist.CLIENT ? Side.CLIENT : Side.SERVER;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public Optional<String> modVersion(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString());
    }

    @Override
    public boolean isDevelopment() {
        return !FMLEnvironment.production;
    }

    /** The registry order, which on NeoForge is the mod construction order. */
    @Override
    public List<ModEntry> modularMods() {
        return ModularModRegistry.entries().stream()
                .map(entry -> new ModEntry(entry.modId(), entry::mod))
                .toList();
    }
}
