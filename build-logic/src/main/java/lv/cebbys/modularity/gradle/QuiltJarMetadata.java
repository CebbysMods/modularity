package lv.cebbys.modularity.gradle;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Adds the nested jars Loom put in {@code META-INF/jars/} to {@code quilt_loader.jars} in a built Quilt jar.
 *
 * <p>Loom writes that list into {@code fabric.mod.json} while it nests the jars, but it does not know
 * {@code quilt.mod.json}. The development resources must not name the nested jars: Quilt Loader reads the metadata
 * from the resources folder and would fail on a file that the jar task has not created yet.
 */
public final class QuiltJarMetadata {

    private static final String METADATA = "quilt.mod.json";
    private static final String NESTED = "META-INF/jars/";
    private static final String LOADER = "\"quilt_loader\": {";

    private QuiltJarMetadata() {
    }

    /** Rewrites {@code jar} in place; does nothing when the metadata has {@code jars} already. */
    public static void addNestedJars(Path jar) {
        try {
            List<String> nested = nestedJars(jar);
            String metadata = read(jar, METADATA);
            if (metadata == null || nested.isEmpty() || metadata.contains("\"jars\"")) {
                return;
            }
            int loader = metadata.indexOf(LOADER);
            int open = loader < 0 ? -1 : metadata.indexOf('{', loader);
            if (open < 0) {
                throw new IllegalStateException(jar + ": " + METADATA + " has no quilt_loader object");
            }
            StringBuilder lines = new StringBuilder("\n    \"jars\": [");
            for (int i = 0; i < nested.size(); i++) {
                lines.append("\n      \"").append(nested.get(i)).append('"');
                if (i + 1 < nested.size()) {
                    lines.append(',');
                }
            }
            lines.append("\n    ],");
            String updated = metadata.substring(0, open + 1) + lines + metadata.substring(open + 1);
            replace(jar, METADATA, updated);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot add the nested jars to " + jar, e);
        }
    }

    private static List<String> nestedJars(Path jar) throws IOException {
        List<String> names = new ArrayList<>();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            for (ZipEntry entry : Collections.list(zip.entries())) {
                String name = entry.getName();
                if (!entry.isDirectory() && name.startsWith(NESTED) && name.endsWith(".jar")) {
                    names.add(name);
                }
            }
        }
        names.sort(null);
        return names;
    }

    private static String read(Path jar, String entryName) throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry entry = zip.getEntry(entryName);
            return entry == null ? null : new String(zip.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void replace(Path jar, String entryName, String content) throws IOException {
        Path temp = jar.resolveSibling(jar.getFileName() + ".tmp");
        try (ZipFile in = new ZipFile(jar.toFile());
             ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(temp))) {
            for (ZipEntry entry : Collections.list(in.entries())) {
                out.putNextEntry(new ZipEntry(entry.getName()));
                if (entry.getName().equals(entryName)) {
                    out.write(content.getBytes(StandardCharsets.UTF_8));
                } else {
                    in.getInputStream(entry).transferTo(out);
                }
                out.closeEntry();
            }
        }
        Files.move(temp, jar, StandardCopyOption.REPLACE_EXISTING);
    }
}
