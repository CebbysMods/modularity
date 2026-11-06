package lv.cebbys.mcmods.mvd;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Finds the running Minecraft version.
 *
 * <pre>{@code
 * DetectedVersion version = VersionDetector.detect();
 * if (version.isAtLeast(1, 21, 0)) { ... }
 * }</pre>
 */
public final class VersionDetector {
    private static volatile DetectedVersion cached;

    private VersionDetector() {
    }

    /**
     * @return the Minecraft version; detected on the first call, then cached
     * @throws VersionDetectionException when every source failed
     */
    public static DetectedVersion detect() {
        DetectedVersion version = cached;
        if (version == null) {
            synchronized (VersionDetector.class) {
                version = cached;
                if (version == null) {
                    version = detect(loadSources());
                    cached = version;
                }
            }
        }
        return version;
    }

    /** Like {@link #detect()}, but empty instead of throwing. */
    public static Optional<DetectedVersion> tryDetect() {
        try {
            return Optional.of(detect());
        } catch (VersionDetectionException e) {
            return Optional.empty();
        }
    }

    /** Runs {@code sources} by descending priority; not cached. */
    static DetectedVersion detect(List<? extends VersionSource> sources) {
        List<VersionSource> ordered = new ArrayList<>(sources);
        ordered.sort(Comparator.comparingInt(VersionSource::priority).reversed());
        List<String> failures = new ArrayList<>();
        for (VersionSource source : ordered) {
            try {
                Optional<String> raw = source.detect();
                if (raw.isPresent()) {
                    return DetectedVersion.parse(raw.get(), source.name());
                }
                failures.add(source.name() + ": not applicable");
            } catch (Throwable t) {
                failures.add(source.name() + ": " + describe(t));
            }
        }
        throw new VersionDetectionException(failures);
    }

    private static List<VersionSource> loadSources() {
        List<VersionSource> sources = new ArrayList<>();
        ServiceLoader.load(VersionSource.class, VersionDetector.class.getClassLoader()).forEach(sources::add);
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
