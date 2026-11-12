package lv.cebbys.mcmods.modularity.neoforge.detect;

import lv.cebbys.mcmods.modularity.anvil.detect.ModularityAnvilPlatformSource;
import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.api.core.PlatformVersion;
import lv.cebbys.mcmods.modularity.api.core.Side;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;

/** Platform source of NeoForge; it reads the loader version, the side and the development flag from FML. */
public final class ModularityNeoForgePlatformSource extends ModularityAnvilPlatformSource {

    @Override
    protected Loader getLoader() {
        return Loader.NEOFORGE;
    }

    @Override
    protected PlatformVersion getVersion() {
        return PlatformVersion.parse(FMLLoader.versionInfo().neoForgeVersion());
    }

    @Override
    protected Side getSide() {
        return FMLEnvironment.dist == Dist.CLIENT ? Side.CLIENT : Side.SERVER;
    }

    @Override
    protected boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }
}
