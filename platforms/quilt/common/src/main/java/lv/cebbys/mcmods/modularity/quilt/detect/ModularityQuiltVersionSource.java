package lv.cebbys.mcmods.modularity.quilt.detect;

import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.loom.detect.ModularityLoomVersionSource;

/** Version source of Quilt. */
public final class ModularityQuiltVersionSource extends ModularityLoomVersionSource {

    @Override
    protected Loader getLoader() {
        return Loader.QUILT;
    }
}
