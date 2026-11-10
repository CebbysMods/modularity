package lv.cebbys.mcmods.modularity.api.detect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Finds the mod loader the game runs on.
 *
 * <pre>{@code
 * if (PlatformDetector.detect().loader() == Loader.FABRIC) { ... }
 * }</pre>
 *
 * <p>A platform installs its own sources before the first detection:
 * {@code PlatformDetector.install(new LoomPlatformSource())}. The built-in marker-class source is
 * always present.
 */
public final class PlatformDetector {
    private static final Object LOCK = new Object();
    private static final Map<String, PlatformSource> INSTALLED = new LinkedHashMap<>();
    static final List<PlatformSource> BUILT_IN = List.of(new MarkerClassSource());

    private static volatile DetectedPlatform cached;

    private PlatformDetector() {
    }

    /**
     * @return the platform; detected on the first call, then cached
     * @throws PlatformDetectionException when every source failed
     */
    public static DetectedPlatform detect() {
        DetectedPlatform platform = cached;
        if (platform == null) {
            synchronized (LOCK) {
                platform = cached;
                if (platform == null) {
                    platform = detect(sources());
                    cached = platform;
                }
            }
        }
        return platform;
    }

    /** Like {@link #detect()}, but empty instead of throwing. */
    public static Optional<DetectedPlatform> tryDetect() {
        try {
            return Optional.of(detect());
        } catch (PlatformDetectionException e) {
            return Optional.empty();
        }
    }

    /**
     * @return the name of the source that produced the cached platform, or {@code none} before the
     *         first detection
     */
    public static String source() {
        DetectedPlatform platform = cached;
        return platform == null ? "none" : platform.source();
    }

    /**
     * Registers {@code sources} for the next and every later detection. A source with the same
     * {@link PlatformSource#name() name} replaces the earlier one. Call this before the first
     * detection; an install after a detection clears the cached result.
     */
    public static void install(PlatformSource... sources) {
        synchronized (LOCK) {
            for (PlatformSource source : sources) {
                INSTALLED.put(source.name(), source);
            }
            cached = null;
        }
    }

    /** Runs {@code sources} by descending priority; not cached. */
    static DetectedPlatform detect(List<? extends PlatformSource> sources) {
        List<PlatformSource> ordered = new ArrayList<>(sources);
        ordered.sort(Comparator.comparingInt(PlatformSource::priority).reversed());
        List<String> failures = new ArrayList<>();
        for (PlatformSource source : ordered) {
            try {
                Optional<DetectedPlatform> platform = source.detect();
                if (platform.isPresent()) {
                    return platform.get();
                }
                failures.add(source.name() + ": not applicable");
            } catch (Throwable t) {
                failures.add(source.name() + ": " + DetectionFailures.describe(t));
            }
        }
        throw new PlatformDetectionException(failures);
    }

    private static List<PlatformSource> sources() {
        List<PlatformSource> sources = new ArrayList<>();
        synchronized (LOCK) {
            sources.addAll(INSTALLED.values());
        }
        sources.addAll(BUILT_IN);
        return sources;
    }

    /** Clears the installed sources and the cached result. For tests. */
    static void reset() {
        synchronized (LOCK) {
            INSTALLED.clear();
            cached = null;
        }
    }
}
