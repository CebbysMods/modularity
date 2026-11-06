package lv.cebbys.modularity.gradle;

import java.util.Arrays;

/**
 * A Minecraft release version ({@code 1.21.1}, {@code 26.1}, {@code 26.1.2}), compared numerically.
 * Platform names use the short form: {@code 1.21.1} is {@code 21.1}, versions from 26.1 on have no {@code 1.} prefix.
 */
public record McVersion(int[] parts) implements Comparable<McVersion> {

    public McVersion {
        if (parts.length < 2) {
            throw new IllegalArgumentException("A version needs at least two parts: " + Arrays.toString(parts));
        }
        parts = parts.clone();
    }

    /** Parses a full Minecraft version, e.g. {@code 1.21.5} or {@code 26.1}. */
    public static McVersion parse(String text) {
        String[] split = text.trim().split("\\.", -1);
        int[] parts = new int[split.length];
        for (int i = 0; i < split.length; i++) {
            if (split[i].isEmpty() || !split[i].chars().allMatch(Character::isDigit)) {
                throw new IllegalArgumentException("Not a Minecraft release version: '" + text + "'");
            }
            parts[i] = Integer.parseInt(split[i]);
        }
        return new McVersion(parts);
    }

    /** Parses a short platform version ({@code 21.1} = 1.21.1, {@code 26.1} = 26.1). */
    public static McVersion parseShort(String text) {
        McVersion v = parse(text);
        return v.parts[0] < 26 ? parse("1." + text) : v;
    }

    /** The short platform form: {@code 1.21.1} gives {@code 21.1}. */
    public String shortName() {
        String full = toString();
        return parts[0] == 1 ? full.substring(2) : full;
    }

    /** Minecraft up to 1.21.11 ships obfuscated (needs remapping), 26.1 and later do not. */
    public boolean obfuscated() {
        return compareTo(parse("26.1")) < 0;
    }

    @Override
    public int compareTo(McVersion other) {
        int n = Math.max(parts.length, other.parts.length);
        for (int i = 0; i < n; i++) {
            int a = i < parts.length ? parts[i] : 0;
            int b = i < other.parts.length ? other.parts[i] : 0;
            if (a != b) {
                return Integer.compare(a, b);
            }
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof McVersion v && compareTo(v) == 0;
    }

    @Override
    public int hashCode() {
        int[] trimmed = parts;
        int end = trimmed.length;
        while (end > 2 && trimmed[end - 1] == 0) {
            end--;
        }
        return Arrays.hashCode(Arrays.copyOf(trimmed, end));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append('.');
            }
            sb.append(parts[i]);
        }
        return sb.toString();
    }
}
