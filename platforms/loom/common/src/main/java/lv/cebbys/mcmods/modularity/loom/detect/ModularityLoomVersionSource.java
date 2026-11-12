package lv.cebbys.mcmods.modularity.loom.detect;

import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.platform.detect.AbstractModularityVersionSource;
import net.fabricmc.loader.api.FabricLoader;

/** Version source of a loader that runs the Fabric Loader API: Fabric and Quilt. */
public abstract class ModularityLoomVersionSource extends AbstractModularityVersionSource {

    protected abstract Loader getLoader();

    @Override
    public String name() {
        return getLoader().id();
    }

    @Override
    protected String getMinecraftVersionString() {
        return FabricLoader.getInstance().getModContainer("minecraft")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElseThrow(() -> new IllegalStateException("Fabric Loader reports no minecraft mod"));
    }
}
