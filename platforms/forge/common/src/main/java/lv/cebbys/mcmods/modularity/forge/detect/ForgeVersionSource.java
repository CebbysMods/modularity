package lv.cebbys.mcmods.modularity.forge.detect;

import lv.cebbys.mcmods.modularity.api.core.MinecraftVersion;
import lv.cebbys.mcmods.modularity.api.detect.VersionSource;
import net.minecraftforge.fml.loading.FMLLoader;

import java.util.Optional;

/** Minecraft version as reported by Forge's mod loader. Only applies when Forge is present. */
public final class ForgeVersionSource implements VersionSource {
    @Override
    public int priority() {
        return 1000;
    }

    @Override
    public String name() {
        return "forge-fml";
    }

    @Override
    public Optional<MinecraftVersion> detect() {
        try {
            Class.forName("net.minecraftforge.fml.loading.FMLLoader", false,
                    ForgeVersionSource.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
        return Optional.ofNullable(FMLLoader.versionInfo().mcVersion()).map(MinecraftVersion::parse);
    }
}
