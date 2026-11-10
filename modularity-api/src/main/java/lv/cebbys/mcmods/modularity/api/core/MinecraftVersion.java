package lv.cebbys.mcmods.modularity.api.core;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minecraft release version ({@code major.minor[.patch]}), e.g. {@code 1.21.1}.
 * Only release versions are modelled; snapshots and pre-releases are rejected.
 */
public final class MinecraftVersion extends Version {
    private static final Pattern PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    private MinecraftVersion(int major, int minor, int patch, String raw) {
        super(major, minor, patch, raw);
    }

    public static MinecraftVersion of(int major, int minor, int patch) {
        return new MinecraftVersion(major, minor, patch, major + "." + minor + "." + patch);
    }

    public static MinecraftVersion of(int major, int minor) {
        return of(major, minor, 0);
    }

    public static MinecraftVersion parse(String value) {
        String trimmed = Objects.requireNonNull(value, "value").trim();
        Matcher matcher = PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Not a Minecraft release version: '" + value + "'");
        }
        int patch = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
        return new MinecraftVersion(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), patch,
                trimmed);
    }

    public boolean isAtLeast(String version) {
        return compareTo(parse(version)) >= 0;
    }

    public boolean isBefore(String version) {
        return compareTo(parse(version)) < 0;
    }

    @Override
    public String toString() {
        return patch() == 0 ? major() + "." + minor() : major() + "." + minor() + "." + patch();
    }
}
