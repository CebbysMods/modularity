package lv.cebbys.mcmods.modularity.loom.entrypoint;

import lv.cebbys.mcmods.modularity.api.detect.PlatformDetector;
import lv.cebbys.mcmods.modularity.api.detect.PlatformSource;
import lv.cebbys.mcmods.modularity.api.detect.VersionDetector;
import lv.cebbys.mcmods.modularity.api.detect.VersionSource;
import lv.cebbys.mcmods.modularity.loom.loader.ModularityLoomLoaderHooks;
import lv.cebbys.mcmods.modularity.loom.loader.ModularityLoomLoaderInfo;
import lv.cebbys.mcmods.modularity.loom.loader.ModularityLoomRenderData;
import lv.cebbys.mcmods.modularity.loom.loader.ModularityLoomStorageBackend;
import lv.cebbys.mcmods.modularity.platform.loader.Loaders;

/**
 * Installs the detection sources and the loader services for the common and the launch stage. Both stages call it, and
 * a second call is harmless. Each loader root passes its own sources, because only that root names its loader.
 */
final class ModularityLoomServices {

    private ModularityLoomServices() {
    }

    static void install(PlatformSource platform, VersionSource version) {
        VersionDetector.install(version);
        PlatformDetector.install(platform);
        Loaders.install(new ModularityLoomLoaderInfo(), new ModularityLoomRenderData(), new ModularityLoomStorageBackend(), new ModularityLoomLoaderHooks());
    }
}
