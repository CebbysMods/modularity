package lv.cebbys.mcmods.modularity.anvil.detect;

import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.platform.detect.AbstractModularityVersionSource;

/**
 * Version source of a Forge-family loader. The class itself names no loader type; a child in the loader root reads the
 * Minecraft version from its own FML (docs/07, 7.3).
 */
public abstract class ModularityAnvilVersionSource extends AbstractModularityVersionSource {

    protected abstract Loader getLoader();

    @Override
    public String name() {
        return getLoader().id();
    }
}
