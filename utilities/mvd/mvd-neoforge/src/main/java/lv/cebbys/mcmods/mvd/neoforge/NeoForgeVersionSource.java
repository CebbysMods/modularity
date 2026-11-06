package lv.cebbys.mcmods.mvd.neoforge;

import lv.cebbys.mcmods.mvd.VersionSource;
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
    public Optional<String> detect() {
        try {
            Class.forName("net.neoforged.fml.loading.FMLLoader", false, NeoForgeVersionSource.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
        return Optional.ofNullable(FMLLoader.versionInfo().mcVersion());
    }
}
