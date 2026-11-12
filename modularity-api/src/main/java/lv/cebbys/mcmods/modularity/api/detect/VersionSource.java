package lv.cebbys.mcmods.modularity.api.detect;

import lv.cebbys.mcmods.modularity.api.core.MinecraftVersion;

import java.util.Optional;

/**
 * One way of finding the Minecraft version. A platform installs its sources with
 * {@link VersionDetector#install(VersionSource...)}; the detector tries them by descending
 * {@link #priority()} and the first one that returns a version wins.
 */
public interface VersionSource {
    /** Higher runs first. Loader sources use 1000, {@code version.json} 100, reflection 0. */
    int priority();

    /** Short name shown in logs and errors: the loader id, e.g. {@code fabric}. */
    String name();

    /**
     * @return the Minecraft version, or empty when this source does not apply
     * @throws Exception when the source applies but failed; reported if every source fails
     */
    Optional<MinecraftVersion> detect() throws Exception;
}
