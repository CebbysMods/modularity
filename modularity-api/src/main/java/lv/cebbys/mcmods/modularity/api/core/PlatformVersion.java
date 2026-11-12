package lv.cebbys.mcmods.modularity.api.core;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Version of a mod loader: Fabric Loader, Quilt Loader, NeoForge or Forge, e.g. {@code 0.20.1},
 * {@code 21.1.265} or {@code 0.20.1+build.42}.
 *
 * <p>The parse is lenient. The numeric prefix fills {@link #major()}, {@link #minor()} and
 * {@link #patch()}; any suffix stays in {@link #raw()}. A text without a numeric prefix, such as
 * {@link #unknown()}, gives zero parts and {@link #isKnown()} is false.
 */
public final class PlatformVersion extends Version {
    private static final String UNKNOWN_TEXT = "unknown";
    private static final Pattern LEADING = Pattern.compile("(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");
    private static final PlatformVersion UNKNOWN = new PlatformVersion(0, 0, 0, UNKNOWN_TEXT);

    private PlatformVersion(int major, int minor, int patch, String raw) {
        super(major, minor, patch, raw);
    }

    /** The version a loader reports when the version is not available. */
    public static PlatformVersion unknown() {
        return UNKNOWN;
    }

    public static PlatformVersion of(int major, int minor, int patch) {
        return new PlatformVersion(major, minor, patch, major + "." + minor + "." + patch);
    }

    public static PlatformVersion parse(String value) {
        String trimmed = Objects.requireNonNull(value, "value").trim();
        if (trimmed.isEmpty()) {
            return UNKNOWN;
        }
        Matcher matcher = LEADING.matcher(trimmed);
        if (!matcher.lookingAt()) {
            return new PlatformVersion(0, 0, 0, trimmed);
        }
        int minor = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
        int patch = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
        return new PlatformVersion(Integer.parseInt(matcher.group(1)), minor, patch, trimmed);
    }

    public boolean isKnown() {
        return !UNKNOWN_TEXT.equals(raw());
    }

    @Override
    public String toString() {
        return raw();
    }
}
