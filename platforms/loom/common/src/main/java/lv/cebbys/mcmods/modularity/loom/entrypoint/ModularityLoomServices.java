package lv.cebbys.mcmods.modularity.loom.entrypoint;

import lv.cebbys.mcmods.modularity.api.detect.PlatformDetector;
import lv.cebbys.mcmods.modularity.api.detect.VersionDetector;
import lv.cebbys.mcmods.modularity.loom.detect.ModularityLoomPlatformSource;
import lv.cebbys.mcmods.modularity.loom.detect.ModularityLoomVersionSource;
import lv.cebbys.mcmods.modularity.loom.loader.ModularityLoomLoaderHooks;
import lv.cebbys.mcmods.modularity.loom.loader.ModularityLoomLoaderInfo;
import lv.cebbys.mcmods.modularity.loom.loader.ModularityLoomRenderData;
import lv.cebbys.mcmods.modularity.loom.loader.ModularityLoomStorageBackend;
import lv.cebbys.mcmods.modularity.platform.loader.Loaders;

/**
 * Installs the detection sources and the loader services for the common and the launch stage. Both stages call it, and
 * a second call is harmless.
 */
final class ModularityLoomServices {

    private ModularityLoomServices() {
    }

    static void install() {
        VersionDetector.install(new ModularityLoomVersionSource());
        PlatformDetector.install(new ModularityLoomPlatformSource());
        Loaders.install(new ModularityLoomLoaderInfo(), new ModularityLoomRenderData(), new ModularityLoomStorageBackend(), new ModularityLoomLoaderHooks());
    }
}
