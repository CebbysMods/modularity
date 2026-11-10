package lv.cebbys.mcmods.modularity.neoforge.detect;

import lv.cebbys.mcmods.modularity.api.core.MinecraftVersion;
import lv.cebbys.mcmods.modularity.api.detect.VersionSource;
import net.neoforged.fml.loading.FMLLoader;

import java.util.Optional;

/** Minecraft version as reported by NeoForge's mod loader. Only applies when NeoForge is present. */
public final class NeoForgeVersionSource implements VersionSource {
    @Override
    public int priority() {
        return 1000;
    }

    @Override
    public String name() {
        return "neoforge-fml";
    }

    @Override
    public Optional<MinecraftVersion> detect() {
        try {
            Class.forName("net.neoforged.fml.loading.FMLLoader", false,
                    NeoForgeVersionSource.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
        return Optional.ofNullable(FMLLoader.versionInfo().mcVersion()).map(MinecraftVersion::parse);
    }
}
