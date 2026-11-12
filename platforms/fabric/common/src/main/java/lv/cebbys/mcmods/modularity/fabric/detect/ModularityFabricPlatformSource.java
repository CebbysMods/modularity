package lv.cebbys.mcmods.modularity.fabric.detect;

import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.loom.detect.ModularityLoomPlatformSource;

/** Platform source of Fabric. */
public final class ModularityFabricPlatformSource extends ModularityLoomPlatformSource {

    @Override
    protected Loader getLoader() {
        return Loader.FABRIC;
    }

    @Override
    protected String getModContainerName() {
        return "fabricloader";
    }
}
