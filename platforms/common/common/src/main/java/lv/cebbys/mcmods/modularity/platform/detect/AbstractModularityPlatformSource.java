package lv.cebbys.mcmods.modularity.platform.detect;

import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.api.core.PlatformVersion;
import lv.cebbys.mcmods.modularity.api.core.Side;
import lv.cebbys.mcmods.modularity.api.detect.DetectedPlatform;
import lv.cebbys.mcmods.modularity.api.detect.PlatformSource;

import java.util.Optional;

/**
 * Base of a platform source that belongs to one loader. It holds the documented loader priority, 1000, and assembles
 * the result; a source answers loader, version, side and development from its own loader API.
 */
public abstract class AbstractModularityPlatformSource implements PlatformSource {

    @Override
    public int priority() {
        return 1000;
    }

    protected abstract Loader getLoader();

    protected abstract PlatformVersion getVersion();

    protected abstract Side getSide();

    protected abstract boolean isDevelopmentEnvironment();

    @Override
    public Optional<DetectedPlatform> detect() {
        return Optional.of(DetectedPlatform.of(getLoader(), getVersion(), getSide(), isDevelopmentEnvironment(), name()));
    }
}
