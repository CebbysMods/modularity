package lv.cebbys.mcmods.mpd;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Finds the mod loader the game runs on.
 *
 * <pre>{@code
 * if (PlatformDetector.detect().platform() == Platform.FABRIC) { ... }
 * }</pre>
 */
public final class PlatformDetector {
    private static volatile DetectedPlatform cached;

    private PlatformDetector() {
    }

    /**
     * @return the platform; detected on the first call, then cached. {@link Platform#UNKNOWN} when no
     * known loader is present.
     * @throws PlatformDetectionException when every source failed
     */
    public static DetectedPlatform detect() {
        DetectedPlatform platform = cached;
        if (platform == null) {
            synchronized (PlatformDetector.class) {
                platform = cached;
                if (platform == null) {
                    platform = detect(loadSources());
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
                failures.add(source.name() + ": " + describe(t));
            }
        }
        throw new PlatformDetectionException(failures);
    }

    private static List<PlatformSource> loadSources() {
        List<PlatformSource> sources = new ArrayList<>();
        ServiceLoader.load(PlatformSource.class, PlatformDetector.class.getClassLoader()).forEach(sources::add);
        return sources;
    }

    /** {@code Type: message -> CauseType: message}, without repeating cycles. */
    static String describe(Throwable error) {
        List<Throwable> chain = new ArrayList<>();
        for (Throwable t = error; t != null && !chain.contains(t); t = t.getCause()) {
            chain.add(t);
        }
        List<String> parts = new ArrayList<>();
        for (Throwable t : chain) {
            parts.add(t.getClass().getSimpleName() + ": " + t.getMessage());
        }
        return String.join(" -> ", parts);
    }
}
