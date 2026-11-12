package lv.cebbys.mcmods.modularity.forge.detect;

import lv.cebbys.mcmods.modularity.anvil.detect.ModularityAnvilPlatformSource;
import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.api.core.PlatformVersion;
import lv.cebbys.mcmods.modularity.api.core.Side;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;

/** Platform source of Forge; it reads the loader version, the side and the development flag from FML. */
public final class ModularityForgePlatformSource extends ModularityAnvilPlatformSource {

    @Override
    protected Loader getLoader() {
        return Loader.FORGE;
    }

    @Override
    protected PlatformVersion getVersion() {
        return PlatformVersion.parse(FMLLoader.versionInfo().forgeVersion());
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
