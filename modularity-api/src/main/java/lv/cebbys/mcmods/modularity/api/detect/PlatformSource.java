package lv.cebbys.mcmods.modularity.api.detect;

import java.util.Optional;

/**
 * One way of finding the platform. A platform installs its sources with
 * {@link PlatformDetector#install(PlatformSource...)}; the detector tries them by descending
 * {@link #priority()} and the first one that returns a platform wins.
 */
public interface PlatformSource {
    /** Higher runs first. Loader sources use 1000, marker classes 0. */
    int priority();

    /** Short name shown in logs and errors: the loader id, e.g. {@code fabric}. */
    String name();

    /**
     * @return the platform, or empty when this source does not apply
     * @throws Exception when the source applies but failed; reported if every source fails
     */
    Optional<DetectedPlatform> detect() throws Exception;
}
