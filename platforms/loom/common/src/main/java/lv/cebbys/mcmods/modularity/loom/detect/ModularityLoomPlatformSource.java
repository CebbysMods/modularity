package lv.cebbys.mcmods.modularity.loom.detect;

import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.api.core.PlatformVersion;
import lv.cebbys.mcmods.modularity.api.core.Side;
import lv.cebbys.mcmods.modularity.platform.detect.AbstractModularityPlatformSource;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Platform source of a loader that runs the Fabric Loader API: Fabric and Quilt. The version, the side and the
 * development flag come from Fabric Loader; a child names its loader and the mod container that carries the loader
 * version (docs/06, 6.1).
 */
public abstract class ModularityLoomPlatformSource extends AbstractModularityPlatformSource {

    protected abstract Loader getLoader();

    /** The mod container that carries the loader version: {@code fabricloader} or {@code quilt_loader}. */
    protected abstract String getModContainerName();

    @Override
    public String name() {
        return getLoader().id();
    }

    @Override
    protected PlatformVersion getVersion() {
        return FabricLoader.getInstance().getModContainer(getModContainerName())
                .map(mod -> PlatformVersion.parse(mod.getMetadata().getVersion().getFriendlyString()))
                .orElse(PlatformVersion.unknown());
    }

    @Override
    protected Side getSide() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ? Side.CLIENT : Side.SERVER;
    }

    @Override
    protected boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}
