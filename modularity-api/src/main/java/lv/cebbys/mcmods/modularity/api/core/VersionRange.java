package lv.cebbys.mcmods.modularity.api.core;

import java.util.Objects;

/**
 * Half-open Minecraft version range {@code [since, until)}. Either bound may be open.
 */
public final class VersionRange {
    private static final VersionRange ANY = new VersionRange(null, null);

    private final MinecraftVersion since;
    private final MinecraftVersion until;

    private VersionRange(MinecraftVersion since, MinecraftVersion until) {
        if (since != null && until != null && since.compareTo(until) >= 0) {
            throw new IllegalArgumentException("Empty version range [" + since + ", " + until + ")");
        }
        this.since = since;
        this.until = until;
    }

    public static VersionRange any() {
        return ANY;
    }

    public static VersionRange since(String version) {
        return new VersionRange(MinecraftVersion.parse(version), null);
    }

    public static VersionRange until(String version) {
        return new VersionRange(null, MinecraftVersion.parse(version));
    }

    public static VersionRange between(String sinceInclusive, String untilExclusive) {
        return new VersionRange(MinecraftVersion.parse(sinceInclusive), MinecraftVersion.parse(untilExclusive));
    }

    public boolean contains(MinecraftVersion version) {
        Objects.requireNonNull(version, "version");
        return (since == null || version.compareTo(since) >= 0) && (until == null || version.compareTo(until) < 0);
    }

    /** Lower bound or {@code null} when open. */
    public MinecraftVersion lowerBound() {
        return since;
    }

    @Override
    public String toString() {
        return "[" + (since == null ? "*" : since) + ", " + (until == null ? "*" : until) + ")";
    }
}
