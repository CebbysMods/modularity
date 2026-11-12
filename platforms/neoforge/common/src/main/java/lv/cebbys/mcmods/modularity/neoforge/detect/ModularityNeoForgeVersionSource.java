package lv.cebbys.mcmods.modularity.neoforge.detect;

import lv.cebbys.mcmods.modularity.anvil.detect.ModularityAnvilVersionSource;
import lv.cebbys.mcmods.modularity.api.core.Loader;
import net.neoforged.fml.loading.FMLLoader;

/** Version source of NeoForge; the FML version info reports the Minecraft version. */
public final class ModularityNeoForgeVersionSource extends ModularityAnvilVersionSource {

    @Override
    protected Loader getLoader() {
        return Loader.NEOFORGE;
    }

    @Override
    protected String getMinecraftVersionString() {
        return FMLLoader.versionInfo().mcVersion();
    }
}
