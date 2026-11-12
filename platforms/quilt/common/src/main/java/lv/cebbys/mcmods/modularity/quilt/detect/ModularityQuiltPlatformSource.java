package lv.cebbys.mcmods.modularity.quilt.detect;

import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.loom.detect.ModularityLoomPlatformSource;

/** Platform source of Quilt. */
public final class ModularityQuiltPlatformSource extends ModularityLoomPlatformSource {

    @Override
    protected Loader getLoader() {
        return Loader.QUILT;
    }

    @Override
    protected String getModContainerName() {
        return "quilt_loader";
    }
}
