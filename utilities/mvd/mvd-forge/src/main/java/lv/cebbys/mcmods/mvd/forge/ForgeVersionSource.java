package lv.cebbys.mcmods.mvd.forge;

import lv.cebbys.mcmods.mvd.VersionSource;
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
    public Optional<String> detect() {
        try {
            Class.forName("net.minecraftforge.fml.loading.FMLLoader", false, ForgeVersionSource.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
        return Optional.ofNullable(FMLLoader.versionInfo().mcVersion());
    }
}
