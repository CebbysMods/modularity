package lv.cebbys.modularity.gradle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayersTest {

    private static final String SUB = "src/main";

    @TempDir
    Path base;

    private void write(String layer, String rel, String content) throws IOException {
        Path f = base.resolve(layer).resolve(SUB).resolve(rel);
        Files.createDirectories(f.getParent());
        Files.writeString(f, content);
    }

    private List<String> labels(String version) {
        return Layers.resolve(base, McVersion.parse(version), SUB).stream().map(Layers.Layer::label).toList();
    }

    @Test
    void layerOrder() throws IOException {
        write("common", "A.java", "a");
        write("changes/1.21.5+", "A.java", "a5");
        write("changes/1.21.2+", "B.java", "b2");
        write("changes/1.21.5-1.21.8", "C.java", "c");
        write("changes/26.1+", "A.java", "a26");
        write("versions/21.6", "D.java", "d");

        assertEquals(List.of("common"), labels("1.21.1"));
        assertEquals(List.of("common", "changes/1.21.2+", "changes/1.21.5-1.21.8", "changes/1.21.5+", "versions/21.6"),
                labels("1.21.6"));
        assertEquals(List.of("common", "changes/1.21.2+", "changes/1.21.5+"), labels("1.21.9"));
        assertEquals(List.of("common", "changes/1.21.2+", "changes/1.21.5+", "changes/26.1+"), labels("26.3"));
    }

    @Test
    void higherLayerWins() throws IOException {
        write("common", "A.java", "a");
        write("common", "pkg/B.java", "b");
        write("changes/1.21.5+", "A.java", "a5");
        write("changes/26.1+", "A.java", "a26");
        write("versions/26.1", "New.java", "n");

        Path out = base.resolve("out");
        Map<String, Layers.Origin> o = Layers.assemble(Layers.resolve(base, McVersion.parse("26.1"), SUB), out);
        assertEquals("changes/26.1+", o.get("A.java").layer());
        assertEquals("common", o.get("pkg/B.java").layer());
        assertEquals("versions/26.1", o.get("New.java").layer());
        assertEquals("a26", Files.readString(out.resolve("A.java")));
        assertEquals("b", Files.readString(out.resolve("pkg/B.java")));

        Layers.assemble(Layers.resolve(base, McVersion.parse("1.21.4"), SUB), out);
        assertEquals("a", Files.readString(out.resolve("A.java")));
        assertFalse(Files.exists(out.resolve("New.java")), "output is cleared between runs");
    }

    @Test
    void identicalCopyFails() throws IOException {
        write("common", "A.java", "same");
        write("changes/1.21.5+", "A.java", "same");
        var e = assertThrows(Layers.LayerException.class,
                () -> Layers.merge(Layers.resolve(base, McVersion.parse("1.21.5"), SUB)));
        assertTrue(e.getMessage().contains("changes/1.21.5+: A.java is identical to the copy in common"), e.getMessage());
    }

    @Test
    void identicalCopyInVersionFolderFails() throws IOException {
        write("common", "A.java", "a");
        write("changes/1.21.5+", "A.java", "a5");
        write("versions/21.6", "A.java", "a5");
        var e = assertThrows(Layers.LayerException.class,
                () -> Layers.merge(Layers.resolve(base, McVersion.parse("1.21.6"), SUB)));
        assertTrue(e.getMessage().contains("versions/21.6: A.java is identical to the copy in changes/1.21.5+"));
    }

    @Test
    void removedMarkerRemovesFileFromBelow() throws IOException {
        write("common", "Old.java", "o");
        write("changes/26.1+", "Old.java.removed", "");
        assertTrue(Layers.merge(Layers.resolve(base, McVersion.parse("1.21.1"), SUB)).containsKey("Old.java"));
        Map<String, Layers.Origin> o = Layers.merge(Layers.resolve(base, McVersion.parse("26.1"), SUB));
        assertFalse(o.containsKey("Old.java"));
        assertFalse(o.containsKey("Old.java.removed"));
    }

    @Test
    void removedMarkerWithoutFileBelowFails() throws IOException {
        write("common", "A.java", "a");
        write("changes/26.1+", "Missing.java.removed", "");
        var e = assertThrows(Layers.LayerException.class,
                () -> Layers.merge(Layers.resolve(base, McVersion.parse("26.1"), SUB)));
        assertTrue(e.getMessage().contains("Missing.java.removed removes Missing.java, which no lower layer has"));
    }

    @Test
    void unparsableRangeFolderFails() throws IOException {
        write("common", "A.java", "a");
        write("changes/1.21.5", "A.java", "x");
        var e = assertThrows(Layers.LayerException.class,
                () -> Layers.resolve(base, McVersion.parse("1.21.5"), SUB));
        assertTrue(e.getMessage().startsWith("changes/1.21.5: Cannot parse version range"), e.getMessage());
    }

    @Test
    void duplicateRangeFails() throws IOException {
        write("changes/26.1+", "A.java", "x");
        write("changes/26.1.0+", "B.java", "y");
        var e = assertThrows(Layers.LayerException.class,
                () -> Layers.resolve(base, McVersion.parse("26.1"), SUB));
        assertTrue(e.getMessage().contains("are the same range"), e.getMessage());
    }

    @Test
    void allErrorsReportedAtOnce() throws IOException {
        write("common", "A.java", "a");
        write("common", "B.java", "b");
        write("changes/1.21.5+", "A.java", "a");
        write("changes/1.21.5+", "B.java", "b");
        var e = assertThrows(Layers.LayerException.class,
                () -> Layers.merge(Layers.resolve(base, McVersion.parse("1.21.5"), SUB)));
        assertEquals(2, e.getMessage().lines().count());
    }

    @Test
    void counts() throws IOException {
        write("common", "A.java", "a");
        write("common", "B.java", "b");
        write("versions/21.1", "C.java", "c");
        assertEquals(Map.of("common", 2, "versions/21.1", 1),
                Layers.counts(Layers.resolve(base, McVersion.parse("1.21.1"), SUB)));
    }
}
