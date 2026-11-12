package lv.cebbys.mcmods.modularity.platform.detect;

import lv.cebbys.mcmods.modularity.api.core.MinecraftVersion;
import lv.cebbys.mcmods.modularity.api.detect.VersionSource;

import java.util.Optional;

/**
 * Base of a version source that belongs to one loader. It holds the documented loader priority, 1000, and parses the
 * version string the loader reports.
 */
public abstract class AbstractModularityVersionSource implements VersionSource {

    @Override
    public int priority() {
        return 1000;
    }

    /** The Minecraft version string the loader reports, e.g. {@code 1.21.1}. */
    protected abstract String getMinecraftVersionString();

    @Override
    public Optional<MinecraftVersion> detect() {
        return Optional.of(MinecraftVersion.parse(getMinecraftVersionString()));
    }
}
