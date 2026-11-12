package lv.cebbys.mcmods.modularity.anvil.detect;

import lv.cebbys.mcmods.modularity.platform.detect.AbstractModularityPlatformSource;

/**
 * Platform source of a Forge-family loader. The class itself names no loader type, because NeoForge and Forge publish
 * their API under different packages (docs/07, 7.3); a child in the loader root reads its own FML.
 */
public abstract class ModularityAnvilPlatformSource extends AbstractModularityPlatformSource {

    @Override
    public String name() {
        return getLoader().id();
    }
}
