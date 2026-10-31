package lv.cebbys.mcmods.modularity.api.core;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minecraft release version ({@code major.minor[.patch]}), e.g. {@code 1.21.1}.
 * Only release versions are modelled; snapshots are out of scope for the API.
 */
public final class McVersion implements Comparable<McVersion> {
    private static final Pattern PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    private final int major;
    private final int minor;
    private final int patch;

    private McVersion(int major, int minor, int patch) {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("Version parts must be positive");
        }
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public static McVersion of(int major, int minor, int patch) {
        return new McVersion(major, minor, patch);
    }

    public static McVersion of(int major, int minor) {
        return new McVersion(major, minor, 0);
    }

    public static McVersion parse(String value) {
        Matcher matcher = PATTERN.matcher(Objects.requireNonNull(value, "value").trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Not a Minecraft release version: '" + value + "'");
        }
        int patch = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
        return new McVersion(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), patch);
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

    public boolean isAtLeast(String version) {
        return compareTo(parse(version)) >= 0;
    }

    public boolean isBefore(String version) {
        return compareTo(parse(version)) < 0;
    }

    @Override
    public int compareTo(McVersion o) {
        if (major != o.major) return Integer.compare(major, o.major);
        if (minor != o.minor) return Integer.compare(minor, o.minor);
        return Integer.compare(patch, o.patch);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof McVersion v && major == v.major && minor == v.minor && patch == v.patch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch);
    }

    @Override
    public String toString() {
        return patch == 0 ? major + "." + minor : major + "." + minor + "." + patch;
    }
}
