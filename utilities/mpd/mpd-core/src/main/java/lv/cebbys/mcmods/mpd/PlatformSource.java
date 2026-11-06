package lv.cebbys.mcmods.mpd;

import java.util.Optional;

/**
 * One way of finding the platform. Sources are found with {@link java.util.ServiceLoader}
 * (register them in {@code META-INF/services/lv.cebbys.mcmods.mpd.PlatformSource}) and tried by
 * descending {@link #priority()}; the first one that returns a result wins.
 */
public interface PlatformSource {
    /** Higher runs first. Loader sources use 1000, marker classes 0. */
    int priority();

    /** Short name shown in logs and errors, e.g. {@code fabric-loader}. */
    String name();

    /**
     * @return the platform, or empty when this source does not apply
     * @throws Exception when the source applies but failed; reported if every source fails
     */
    Optional<DetectedPlatform> detect() throws Exception;
}
