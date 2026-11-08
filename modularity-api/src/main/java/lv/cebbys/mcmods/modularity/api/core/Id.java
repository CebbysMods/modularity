package lv.cebbys.mcmods.modularity.api.core;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Loader and version independent resource identifier ({@code namespace:path}).
 * Maps 1:1 to Minecraft's {@code ResourceLocation} / {@code Identifier}.
 */
public final class Id implements Comparable<Id> {
    public static final String MINECRAFT = "minecraft";
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9_./-]+");

    private final String namespace;
    private final String path;

    private Id(String namespace, String path) {
        if (!NAMESPACE.matcher(namespace).matches()) {
            throw new IllegalArgumentException("Invalid namespace '" + namespace + "'");
        }
        if (!PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("Invalid path '" + path + "'");
        }
        this.namespace = namespace;
        this.path = path;
    }

    public static Id of(String namespace, String path) {
        return new Id(Objects.requireNonNull(namespace, "namespace"), Objects.requireNonNull(path, "path"));
    }

    public static Id minecraft(String path) {
        return of(MINECRAFT, path);
    }

    /**
     * Parses {@code namespace:path}. A missing namespace defaults to {@code minecraft}, like vanilla does.
     */
    public static Id parse(String value) {
        Objects.requireNonNull(value, "value");
        int separator = value.indexOf(':');
        if (separator < 0) {
            return minecraft(value);
        }
        return of(value.substring(0, separator), value.substring(separator + 1));
    }

    public String namespace() {
        return namespace;
    }

    public String path() {
        return path;
    }

    public Id withPath(String newPath) {
        return of(namespace, newPath);
    }

    public Id withPrefix(String prefix) {
        return of(namespace, prefix + path);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Id other && namespace.equals(other.namespace) && path.equals(other.path);
    }

    @Override
    public int hashCode() {
        return 31 * namespace.hashCode() + path.hashCode();
    }

    @Override
    public int compareTo(Id o) {
        int result = namespace.compareTo(o.namespace);
        return result != 0 ? result : path.compareTo(o.path);
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
