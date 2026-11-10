package lv.cebbys.mcmods.modularity.api.core;

import java.util.Objects;

/**
 * A numeric version. Two kinds exist: a {@link MinecraftVersion} and a {@link PlatformVersion}.
 * The two kinds describe different things, so they never compare equal and never compare to each
 * other. A comparison across the two kinds throws {@link IllegalArgumentException}.
 */
public sealed abstract class Version implements Comparable<Version> permits MinecraftVersion, PlatformVersion {
    private final int major;
    private final int minor;
    private final int patch;
    private final String raw;

    protected Version(int major, int minor, int patch, String raw) {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("Version parts must be positive");
        }
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.raw = Objects.requireNonNull(raw, "raw");
    }

    public final int major() {
        return major;
    }

    public final int minor() {
        return minor;
    }

    public final int patch() {
        return patch;
    }

    /** The version text as the source reported it, e.g. {@code 1.21.1} or {@code 0.20.1+build.42}. */
    public final String raw() {
        return raw;
    }

    public final boolean isAtLeast(int major, int minor, int patch) {
        return compareParts(major, minor, patch) >= 0;
    }

    @Override
    public final int compareTo(Version other) {
        Objects.requireNonNull(other, "other");
        if (getClass() != other.getClass()) {
            throw new IllegalArgumentException(
                    "Cannot compare a " + getClass().getSimpleName() + " with a " + other.getClass().getSimpleName());
        }
        return compareParts(other.major, other.minor, other.patch);
    }

    private int compareParts(int major, int minor, int patch) {
        if (this.major != major) return Integer.compare(this.major, major);
        if (this.minor != minor) return Integer.compare(this.minor, minor);
        return Integer.compare(this.patch, patch);
    }

    @Override
    public final boolean equals(Object o) {
        return this == o || o != null && getClass() == o.getClass()
                && compareParts(((Version) o).major, ((Version) o).minor, ((Version) o).patch) == 0;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(getClass(), major, minor, patch);
    }

    @Override
    public abstract String toString();
}
