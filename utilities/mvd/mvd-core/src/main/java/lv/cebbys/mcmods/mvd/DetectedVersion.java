package lv.cebbys.mcmods.mvd;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Minecraft release version ({@code major.minor[.patch]}), such as {@code 1.21.1} or the year based
 * {@code 26.1.2}, together with the source that found it. A missing patch counts as 0.
 */
public final class DetectedVersion implements Comparable<DetectedVersion> {
    private static final Pattern PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    private final int major;
    private final int minor;
    private final int patch;
    private final String raw;
    private final String source;

    private DetectedVersion(int major, int minor, int patch, String raw, String source) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.raw = raw;
        this.source = source;
    }

    /**
     * @param raw    version string, e.g. {@code 1.21.1}
     * @param source name of the source that produced it
     * @throws IllegalArgumentException when {@code raw} is not a release version (snapshots, pre-releases)
     */
    public static DetectedVersion parse(String raw, String source) {
        Objects.requireNonNull(raw, "raw");
        Objects.requireNonNull(source, "source");
        String trimmed = raw.trim();
        Matcher matcher = PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "'" + raw + "' does not match the patterns 'major.minor' or 'major.minor.patch'");
        }
        int patch = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
        return new DetectedVersion(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), patch,
                trimmed, source);
    }

    public int major() {
        return major;
    }

    public int minor() {
        return minor;
    }

    public int patch() {
        return patch;
    }

    /** The version string as the source reported it. */
    public String raw() {
        return raw;
    }

    /** Name of the {@link VersionSource} that found this version. */
    public String source() {
        return source;
    }

    public boolean isAtLeast(int major, int minor, int patch) {
        return compare(major, minor, patch) >= 0;
    }

    private int compare(int major, int minor, int patch) {
        if (this.major != major) {
            return Integer.compare(this.major, major);
        }
        if (this.minor != minor) {
            return Integer.compare(this.minor, minor);
        }
        return Integer.compare(this.patch, patch);
    }

    /** Orders by major, minor, patch; the source is ignored. */
    @Override
    public int compareTo(DetectedVersion other) {
        return compare(other.major, other.minor, other.patch);
    }

    /** Equal when major, minor and patch are equal ({@code 1.21} equals {@code 1.21.0}). */
    @Override
    public boolean equals(Object o) {
        return o instanceof DetectedVersion other && compareTo(other) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch);
    }

    /** The version as reported, e.g. {@code 1.21.1}. */
    @Override
    public String toString() {
        return raw;
    }
}
