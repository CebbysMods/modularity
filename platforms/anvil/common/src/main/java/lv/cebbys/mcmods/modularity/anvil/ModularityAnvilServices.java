package lv.cebbys.mcmods.modularity.anvil;

import lv.cebbys.mcmods.modularity.anvil.detect.ModularityAnvilPlatformSource;
import lv.cebbys.mcmods.modularity.anvil.detect.ModularityAnvilVersionSource;
import lv.cebbys.mcmods.modularity.api.detect.PlatformDetector;
import lv.cebbys.mcmods.modularity.api.detect.VersionDetector;
import lv.cebbys.mcmods.modularity.platform.loader.LoaderHooks;
import lv.cebbys.mcmods.modularity.platform.loader.LoaderInfo;
import lv.cebbys.mcmods.modularity.platform.loader.Loaders;
import lv.cebbys.mcmods.modularity.platform.loader.RenderDataAccess;
import lv.cebbys.mcmods.modularity.platform.loader.StorageBackend;

import java.util.Objects;

/**
 * The shared services of a Forge-family platform: the detection sources and the four loader interfaces. Each loader
 * root builds its own parts, because the two loader APIs do not share a type; the launch and the common stage install
 * them through {@link #install()}.
 */
public record ModularityAnvilServices(LoaderInfo info, RenderDataAccess renderData, StorageBackend storage,
                                      LoaderHooks hooks) {

    public ModularityAnvilServices {
        Objects.requireNonNull(info, "info");
        Objects.requireNonNull(renderData, "renderData");
        Objects.requireNonNull(storage, "storage");
        Objects.requireNonNull(hooks, "hooks");
    }

    /**
     * Installs the detection sources and every service the launch and the common stage use. The client service is
     * installed by the client stage. A second call is harmless.
     */
    public void install() {
        VersionDetector.install(new ModularityAnvilVersionSource());
        PlatformDetector.install(new ModularityAnvilPlatformSource());
        Loaders.install(info, renderData, storage, hooks);
    }
}
