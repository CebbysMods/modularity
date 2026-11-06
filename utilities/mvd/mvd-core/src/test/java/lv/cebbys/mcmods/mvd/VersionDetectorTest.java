package lv.cebbys.mcmods.mvd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionDetectorTest {
    private interface Detect {
        Optional<String> run() throws Exception;
    }

    private static VersionSource source(String name, int priority, Detect detect) {
        return new VersionSource() {
            @Override
            public int priority() {
                return priority;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public Optional<String> detect() throws Exception {
                return detect.run();
            }
        };
    }

    @Test
    void parsesClassicVersions() {
        DetectedVersion version = DetectedVersion.parse("1.21.1", "test");
        assertEquals(1, version.major());
        assertEquals(21, version.minor());
        assertEquals(1, version.patch());
        assertEquals("1.21.1", version.toString());
        assertEquals("test", version.source());
    }

    @Test
    void parsesYearBasedVersions() {
        DetectedVersion version = DetectedVersion.parse("26.1.2", "test");
        assertEquals(26, version.major());
        assertEquals(1, version.minor());
        assertEquals(2, version.patch());
        assertTrue(version.compareTo(DetectedVersion.parse("1.21.11", "test")) > 0);
    }

    @Test
    void missingPatchIsZero() {
        DetectedVersion version = DetectedVersion.parse(" 1.21 ", "test");
        assertEquals(0, version.patch());
        assertEquals("1.21", version.raw());
        assertEquals(DetectedVersion.parse("1.21.0", "other"), version);
        assertTrue(version.isAtLeast(1, 21, 0));
        assertFalse(version.isAtLeast(1, 21, 1));
    }

    @Test
    void rejectsNonReleaseVersions() {
        for (String raw : List.of("24w14a", "1.21-pre1", "1.21.1-rc1", "1", "1.21.1.1", "", "+1.21")) {
            assertThrows(IllegalArgumentException.class, () -> DetectedVersion.parse(raw, "test"), raw);
        }
    }

    @Test
    void highestPrioritySourceWins() {
        DetectedVersion version = VersionDetector.detect(List.of(
                source("low", 0, () -> Optional.of("1.20.1")),
                source("high", 1000, () -> Optional.of("1.21.1")),
                source("mid", 100, () -> Optional.of("1.19.4"))));
        assertEquals("1.21.1", version.raw());
        assertEquals("high", version.source());
    }

    @Test
    void fallsThroughEmptyFailingAndUnparseableSources() {
        DetectedVersion version = VersionDetector.detect(List.of(
                source("empty", 1000, Optional::empty),
                source("broken", 500, () -> {
                    throw new IllegalStateException("boom");
                }),
                source("snapshot", 200, () -> Optional.of("24w14a")),
                source("good", 100, () -> Optional.of("1.21.1"))));
        assertEquals("good", version.source());
    }

    @Test
    void reportsEverySourceWhenAllFail() {
        VersionDetectionException error = assertThrows(VersionDetectionException.class, () -> VersionDetector.detect(List.of(
                source("second", 10, () -> {
                    throw new IllegalStateException("outer", new ClassNotFoundException("inner"));
                }),
                source("first", 20, Optional::empty),
                source("third", 0, () -> Optional.of("not-a-version")))));
        assertEquals(3, error.failures().size());
        assertEquals("first: not applicable", error.failures().get(0));
        assertEquals("second: IllegalStateException: outer -> ClassNotFoundException: inner", error.failures().get(1));
        assertTrue(error.failures().get(2).startsWith("third: IllegalArgumentException"));
        assertTrue(error.getMessage().contains(VersionDetectionException.ISSUES));
    }

    @Test
    void noSourcesIsReported() {
        VersionDetectionException error = assertThrows(VersionDetectionException.class,
                () -> VersionDetector.detect(List.of()));
        assertTrue(error.getMessage().contains("no version sources"));
    }

    @Test
    void versionJsonIdIsRead() {
        String json = "{\n  \"id\": \"1.21.1\",\n  \"name\": \"1.21.1\",\n  \"world_version\": 3955,\n  \"protocol_version\": 767\n}";
        assertEquals(Optional.of("1.21.1"), VersionJsonSource.parseId(json));
        assertEquals(Optional.empty(), VersionJsonSource.parseId("{\"id\": \"some-other-file\"}"));
    }

    @Test
    void versionJsonIsFoundOnTheClassPath(@TempDir Path dir) throws Exception {
        Path other = Files.createDirectories(dir.resolve("other"));
        Files.writeString(other.resolve("version.json"), "{\"id\": \"unrelated\"}");
        Path game = Files.createDirectories(dir.resolve("game"));
        Files.writeString(game.resolve("version.json"), "{\"id\": \"26.1.2\", \"world_version\": 1}");
        try (URLClassLoader loader = new URLClassLoader(
                new URL[]{other.toUri().toURL(), game.toUri().toURL()}, null)) {
            assertEquals(Optional.of("26.1.2"), new VersionJsonSource(loader).detect());
        }
    }

    @Test
    void reflectionDoesNotApplyWithoutGameClasses() throws Exception {
        try (URLClassLoader loader = new URLClassLoader(new URL[0], null)) {
            assertEquals(Optional.empty(), new ReflectionSource(loader).detect());
        }
    }

    @Test
    void builtInSourcesAreRegistered() {
        List<String> names = java.util.ServiceLoader.load(VersionSource.class).stream()
                .map(provider -> provider.get().name()).toList();
        assertEquals(List.of("version-json", "reflection"), names);
    }
}
