package lv.cebbys.mcmods.modularity.fabric.detect;

import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.loom.detect.ModularityLoomVersionSource;

/** Version source of Fabric. */
public final class ModularityFabricVersionSource extends ModularityLoomVersionSource {

    @Override
    protected Loader getLoader() {
        return Loader.FABRIC;
    }
}
