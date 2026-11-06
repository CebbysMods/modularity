package lv.cebbys.modularity.gradle;

import java.util.Comparator;

/**
 * Name of a {@code changes/<range>} folder: {@code 1.21.5+} (that version and every later one) or
 * {@code 1.21.5-1.21.8} (inclusive). Ordered by start, then end, open ranges last.
 */
public record VersionRange(McVersion start, McVersion end) implements Comparable<VersionRange> {

    public static final Comparator<VersionRange> ORDER = Comparator
            .comparing(VersionRange::start)
            .thenComparing(VersionRange::end, Comparator.nullsLast(Comparator.naturalOrder()));

    public VersionRange {
        if (end != null && end.compareTo(start) < 0) {
            throw new IllegalArgumentException("Range ends before it starts: " + start + "-" + end);
        }
    }

    public static VersionRange parse(String name) {
        try {
            if (name.endsWith("+")) {
                return new VersionRange(McVersion.parse(name.substring(0, name.length() - 1)), null);
            }
            int dash = name.indexOf('-');
            if (dash > 0 && name.indexOf('-', dash + 1) < 0) {
                return new VersionRange(McVersion.parse(name.substring(0, dash)), McVersion.parse(name.substring(dash + 1)));
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Cannot parse version range '" + name + "': " + e.getMessage(), e);
        }
        throw new IllegalArgumentException("Cannot parse version range '" + name
                + "': expected '<version>+' or '<from>-<to>', e.g. 1.21.5+ or 1.21.5-1.21.8");
    }

    public boolean contains(McVersion version) {
        return version.compareTo(start) >= 0 && (end == null || version.compareTo(end) <= 0);
    }

    @Override
    public int compareTo(VersionRange other) {
        return ORDER.compare(this, other);
    }

    @Override
    public String toString() {
        return end == null ? start + "+" : start + "-" + end;
    }
}
