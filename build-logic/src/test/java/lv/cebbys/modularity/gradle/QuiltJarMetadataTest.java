package lv.cebbys.modularity.gradle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@code quilt_loader.jars} is added to the built jar, not to the development resources. */
class QuiltJarMetadataTest {

    private static final String QUILT_MOD = """
            {
              "schema_version": 1,
              "quilt_loader": {
                "id": "modularity"
              }
            }
            """;

    @Test
    void addsNestedJarsInsideQuiltLoader(@TempDir Path dir) throws IOException {
        Path jar = dir.resolve("mod.jar");
        write(jar, Map.of(
                "quilt.mod.json", QUILT_MOD.getBytes(StandardCharsets.UTF_8),
                "META-INF/jars/b.jar", new byte[] {2},
                "META-INF/jars/a.jar", new byte[] {1}));

        QuiltJarMetadata.addNestedJars(jar);

        String json = read(jar, "quilt.mod.json");
        assertTrue(json.contains("""
                "quilt_loader": {
                    "jars": [
                      "META-INF/jars/a.jar",
                      "META-INF/jars/b.jar"
                    ],
                    "id": "modularity"
                """), json);
        // The classes of the nested jars are still in the jar, and a second pass adds nothing.
        assertEquals(1, read(jar, "META-INF/jars/a.jar").length());
        QuiltJarMetadata.addNestedJars(jar);
        assertEquals(json, read(jar, "quilt.mod.json"));
    }

    @Test
    void jarWithoutNestedJarsIsUntouched(@TempDir Path dir) throws IOException {
        Path jar = dir.resolve("mod.jar");
        write(jar, Map.of("quilt.mod.json", QUILT_MOD.getBytes(StandardCharsets.UTF_8)));
        QuiltJarMetadata.addNestedJars(jar);
        assertEquals(QUILT_MOD, read(jar, "quilt.mod.json"));
    }

    private static void write(Path jar, Map<String, byte[]> entries) throws IOException {
        Files.createDirectories(jar.getParent());
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
            for (Map.Entry<String, byte[]> entry : new LinkedHashMap<>(entries).entrySet()) {
                out.putNextEntry(new ZipEntry(entry.getKey()));
                out.write(entry.getValue());
                out.closeEntry();
            }
        }
    }

    private static String read(Path jar, String name) throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile());
             InputStream in = zip.getInputStream(zip.getEntry(name))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
