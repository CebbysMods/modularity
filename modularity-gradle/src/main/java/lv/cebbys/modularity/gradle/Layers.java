package lv.cebbys.modularity.gradle;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Layered sources (docs/07-multi-version-build.md, 7.3). For version V the layers are, lowest first:
 * {@code common}, every {@code changes/<range>} containing V (by range start, then end, open last), and
 * {@code versions/<V>}. A higher layer replaces the file with the same relative path; {@code X.removed} removes X.
 */
public final class Layers {

    public static final String REMOVED = ".removed";

    /** One layer: a label for messages (e.g. {@code changes/1.21.5+}) and its directory. */
    public record Layer(String label, Path dir) {
    }

    /** Where the winning file of a path comes from. */
    public record Origin(String layer, Path file) {
    }

    private Layers() {
    }

    /**
     * The layers of {@code version} under {@code base} (e.g. {@code platforms/fabric}), each pointing at
     * {@code <layer>/<sub>} (e.g. {@code src/main}). Missing directories are skipped.
     *
     * @throws LayerException for an unparsable or duplicate range folder name
     */
    public static List<Layer> resolve(Path base, McVersion version, String sub) {
        List<Layer> layers = new ArrayList<>();
        layers.add(new Layer("common", base.resolve("common").resolve(sub)));

        Map<VersionRange, String> ranges = new TreeMap<>();
        Path changes = base.resolve("changes");
        if (Files.isDirectory(changes)) {
            for (Path dir : list(changes)) {
                if (!Files.isDirectory(dir)) {
                    continue;
                }
                String name = dir.getFileName().toString();
                VersionRange range;
                try {
                    range = VersionRange.parse(name);
                } catch (IllegalArgumentException e) {
                    throw new LayerException("changes/" + name + ": " + e.getMessage());
                }
                String previous = ranges.put(range, name);
                if (previous != null) {
                    throw new LayerException("changes/" + previous + " and changes/" + name + " are the same range "
                            + range + "; merge them");
                }
            }
        }
        ranges.forEach((range, name) -> {
            if (range.contains(version)) {
                layers.add(new Layer("changes/" + name, changes.resolve(name).resolve(sub)));
            }
        });

        layers.add(new Layer("versions/" + version.shortName(), base.resolve("versions").resolve(version.shortName())
                .resolve(sub)));
        return layers.stream().filter(l -> Files.isDirectory(l.dir())).toList();
    }

    /**
     * The layers of {@code version} for a stack of layer roots under {@code base} (e.g. {@code platforms} with
     * {@code minecraft, fabric}): every root's layers in root order, labelled {@code <root>/<layer>}. Missing roots
     * contribute nothing.
     */
    public static List<Layer> resolve(Path base, List<String> roots, McVersion version, String sub) {
        List<Layer> layers = new ArrayList<>();
        for (String root : roots) {
            for (Layer layer : resolve(base.resolve(root), version, sub)) {
                layers.add(new Layer(root + "/" + layer.label(), layer.dir()));
            }
        }
        return layers;
    }

    /**
     * Merges the layers (lowest first) into relative path to origin.
     *
     * @throws LayerException when a layer contains a copy identical to the file below it, or a {@code .removed}
     *                        marker for a file that does not exist below
     */
    public static Map<String, Origin> merge(List<Layer> layers) {
        Map<String, Origin> result = new TreeMap<>();
        List<String> errors = new ArrayList<>();
        for (Layer layer : layers) {
            for (Path file : files(layer.dir())) {
                String rel = layer.dir().relativize(file).toString().replace('\\', '/');
                if (rel.endsWith(REMOVED)) {
                    String target = rel.substring(0, rel.length() - REMOVED.length());
                    if (result.remove(target) == null) {
                        errors.add(layer.label() + ": " + rel + " removes " + target + ", which no lower layer has");
                    }
                    continue;
                }
                Origin below = result.get(rel);
                if (below != null && sameContent(below.file(), file)) {
                    errors.add(layer.label() + ": " + rel + " is identical to the copy in " + below.layer()
                            + "; delete it (only changes belong in a higher layer)");
                }
                result.put(rel, new Origin(layer.label(), file));
            }
        }
        if (!errors.isEmpty()) {
            throw new LayerException(String.join("\n", errors));
        }
        return result;
    }

    /** Copies the merged files into {@code out} (cleared first) and returns relative path to origin. */
    public static Map<String, Origin> assemble(List<Layer> layers, Path out) {
        Map<String, Origin> merged = merge(layers);
        try {
            if (Files.exists(out)) {
                try (Stream<Path> walk = Files.walk(out)) {
                    for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                        Files.delete(p);
                    }
                }
            }
            Files.createDirectories(out);
            for (Map.Entry<String, Origin> e : merged.entrySet()) {
                Path target = out.resolve(e.getKey());
                Files.createDirectories(target.getParent());
                Files.copy(e.getValue().file(), target);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return merged;
    }

    /** Files per layer label, for reports. */
    public static Map<String, Integer> counts(List<Layer> layers) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        layers.forEach(l -> counts.put(l.label(), files(l.dir()).size()));
        return counts;
    }

    private static boolean sameContent(Path a, Path b) {
        try {
            return Files.size(a) == Files.size(b) && Files.mismatch(a, b) < 0;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Path> files(Path dir) {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(Files::isRegularFile).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Path> list(Path dir) {
        try (Stream<Path> s = Files.list(dir)) {
            return s.sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A broken layer layout; the message says which file or folder to fix. */
    public static final class LayerException extends RuntimeException {
        public LayerException(String message) {
            super(message);
        }
    }
}
