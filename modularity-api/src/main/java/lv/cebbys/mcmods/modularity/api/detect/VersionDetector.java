package lv.cebbys.mcmods.modularity.api.detect;

import lv.cebbys.mcmods.modularity.api.core.MinecraftVersion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Finds the running Minecraft version.
 *
 * <pre>{@code
 * MinecraftVersion version = VersionDetector.detect();
 * if (version.isAtLeast("1.21.2")) { ... }
 * }</pre>
 *
 * <p>A platform installs its own sources before the first detection:
 * {@code VersionDetector.install(new ModularityLoomVersionSource())}. The built-in sources
 * ({@code version.json} and reflection) are always present.
 */
public final class VersionDetector {
    private static final Object LOCK = new Object();
    private static final Map<String, VersionSource> INSTALLED = new LinkedHashMap<>();
    static final List<VersionSource> BUILT_IN = List.of(new VersionJsonSource(), new ReflectionSource());

    private static volatile Result cached;

    private VersionDetector() {
    }

    /**
     * @return the Minecraft version; detected on the first call, then cached
     * @throws VersionDetectionException when every source failed
     */
    public static MinecraftVersion detect() {
        return result().version();
    }

    /** Like {@link #detect()}, but empty instead of throwing. */
    public static Optional<MinecraftVersion> tryDetect() {
        try {
            return Optional.of(detect());
        } catch (VersionDetectionException e) {
            return Optional.empty();
        }
    }

    /**
     * @return the name of the source that produced the cached version, or {@code none} before the
     *         first detection
     */
    public static String source() {
        Result result = cached;
        return result == null ? "none" : result.source();
    }

    /**
     * Registers {@code sources} for the next and every later detection. A source with the same
     * {@link VersionSource#name() name} replaces the earlier one. Call this before the first
     * detection; an install after a detection clears the cached result.
     */
    public static void install(VersionSource... sources) {
        synchronized (LOCK) {
            for (VersionSource source : sources) {
                INSTALLED.put(source.name(), source);
            }
            cached = null;
        }
    }

    private static Result result() {
        Result result = cached;
        if (result == null) {
            synchronized (LOCK) {
                result = cached;
                if (result == null) {
                    result = detect(sources());
                    cached = result;
                }
            }
        }
        return result;
    }

    /** Runs {@code sources} by descending priority; not cached. */
    static Result detect(List<? extends VersionSource> sources) {
        List<VersionSource> ordered = new ArrayList<>(sources);
        ordered.sort(Comparator.comparingInt(VersionSource::priority).reversed());
        List<String> failures = new ArrayList<>();
        for (VersionSource source : ordered) {
            try {
                Optional<MinecraftVersion> version = source.detect();
                if (version.isPresent()) {
                    return new Result(version.get(), source.name());
                }
                failures.add(source.name() + ": not applicable");
            } catch (Throwable t) {
                failures.add(source.name() + ": " + DetectionFailures.describe(t));
            }
        }
        throw new VersionDetectionException(failures);
    }

    private static List<VersionSource> sources() {
        List<VersionSource> sources = new ArrayList<>();
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

    /** A detected version and the name of the source that found it. */
    record Result(MinecraftVersion version, String source) {
    }
}
