package lv.cebbys.mcmods.modularity.platform;

import lv.cebbys.mcmods.modularity.api.LaunchContext;
import lv.cebbys.mcmods.modularity.api.ModularityModCommon;
import lv.cebbys.mcmods.modularity.api.ModularityModLaunch;
import lv.cebbys.mcmods.modularity.api.core.Side;
import lv.cebbys.mcmods.modularity.api.detect.DetectedPlatform;
import lv.cebbys.mcmods.modularity.api.detect.PlatformDetector;
import lv.cebbys.mcmods.modularity.platform.loader.LoaderInfo;
import lv.cebbys.mcmods.modularity.platform.loader.Loaders;

import java.util.Optional;

/**
 * Runs the launch stage of every mod built on Modularity, before the game starts. A loader calls it after the mods
 * exist, including every guest mod it adds. A mod without the stage is skipped; the runtime is not installed yet, so
 * the context gives no {@link lv.cebbys.mcmods.modularity.api.Platform}.
 */
public final class ModularityLaunch {
    private ModularityLaunch() {
    }

    public static void init(String loaderName) {
        for (LoaderInfo.ModEntry entry : Loaders.info().modularMods()) {
            ModularityCommon.run(entry.modId(), "to launch", () -> {
                ModularityModCommon mod = entry.mod().get();
                if (mod instanceof ModularityModLaunch stage) {
                    stage.initialize(new Context(entry.modId(), loaderName));
                }
            });
        }
    }

    private record Context(String modId, String loaderName) implements LaunchContext {
        @Override
        public System.Logger logger() {
            return System.getLogger(modId);
        }

        @Override
        public Optional<Side> side() {
            return PlatformDetector.tryDetect().flatMap(DetectedPlatform::side);
        }

        @Override
        public String loader() {
            return loaderName;
        }
    }
}
