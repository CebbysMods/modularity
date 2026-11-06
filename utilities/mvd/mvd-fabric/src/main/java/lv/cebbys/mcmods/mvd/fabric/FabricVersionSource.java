package lv.cebbys.mcmods.mvd.fabric;

import lv.cebbys.mcmods.mvd.VersionSource;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Optional;

/** Version of Fabric's built-in {@code minecraft} mod. Only applies when Fabric Loader is present. */
public final class FabricVersionSource implements VersionSource {
    @Override
    public int priority() {
        return 1000;
    }

    @Override
    public String name() {
        return "fabric-loader";
    }

    @Override
    public Optional<String> detect() {
        try {
            Class.forName("net.fabricmc.loader.api.FabricLoader", false, FabricVersionSource.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
        return FabricLoader.getInstance().getModContainer("minecraft")
                .map(container -> container.getMetadata().getVersion().getFriendlyString());
    }
}
