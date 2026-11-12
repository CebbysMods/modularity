package lv.cebbys.mcmods.modularity.testkit;

import lv.cebbys.mcmods.modularity.api.Platform;
import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.api.core.MinecraftVersion;
import lv.cebbys.mcmods.modularity.api.core.Side;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Configurable {@link Platform} for tests. */
public final class TestPlatform implements Platform {
    private final Map<String, String> mods = new HashMap<>();
    private MinecraftVersion version = MinecraftVersion.parse("1.21.1");
    private Side side = Side.CLIENT;

    public TestPlatform minecraftVersion(String version) {
        this.version = MinecraftVersion.parse(version);
        return this;
    }

    public TestPlatform side(Side side) {
        this.side = side;
        return this;
    }

    public TestPlatform withMod(String modId, String version) {
        mods.put(modId, version);
        return this;
    }

    @Override
    public Loader loader() {
        return Loader.TEST;
    }

    @Override
    public MinecraftVersion minecraftVersion() {
        return version;
    }

    @Override
    public Side side() {
        return side;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return mods.containsKey(modId);
    }

    @Override
    public Optional<String> modVersion(String modId) {
        return Optional.ofNullable(mods.get(modId));
    }

    @Override
    public boolean isDevelopment() {
        return true;
    }
}
