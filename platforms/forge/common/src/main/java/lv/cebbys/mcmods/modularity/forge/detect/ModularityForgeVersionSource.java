package lv.cebbys.mcmods.modularity.forge.detect;

import lv.cebbys.mcmods.modularity.anvil.detect.ModularityAnvilVersionSource;
import lv.cebbys.mcmods.modularity.api.core.Loader;
import net.minecraftforge.fml.loading.FMLLoader;

/** Version source of Forge; the FML version info reports the Minecraft version. */
public final class ModularityForgeVersionSource extends ModularityAnvilVersionSource {

    @Override
    protected Loader getLoader() {
        return Loader.FORGE;
    }

    @Override
    protected String getMinecraftVersionString() {
        return FMLLoader.versionInfo().mcVersion();
    }
}
