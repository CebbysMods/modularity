package lv.cebbys.mcmods.modularity.api.detect;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import lv.cebbys.mcmods.modularity.api.core.MinecraftVersion;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Epics;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Features;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Issues;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Stories;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Tags;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Epic(Epics.MODULARITY)
@Feature(Features.L1_UNIT)
@Story(Stories.RUNTIME_DETECTION)
@Issue(Issues.PR_9)
@Tag(Tags.PR_9)
class VersionDetectorTest {
    private interface Detect {
        Optional<MinecraftVersion> run() throws Exception;
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
            public Optional<MinecraftVersion> detect() throws Exception {
                return detect.run();
            }
        };
    }

    @Test
    void highestPrioritySourceWins() {
        VersionDetector.Result result = VersionDetector.detect(List.of(
                source("low", 0, () -> Optional.of(MinecraftVersion.parse("1.20.1"))),
                source("high", 1000, () -> Optional.of(MinecraftVersion.parse("1.21.1"))),
                source("mid", 100, () -> Optional.of(MinecraftVersion.parse("1.19.4")))));
        assertEquals("1.21.1", result.version().toString());
        assertEquals("high", result.source());
    }

    @Test
    void fallsThroughEmptyFailingAndUnparseableSources() {
        VersionDetector.Result result = VersionDetector.detect(List.of(
                source("empty", 1000, Optional::empty),
                source("broken", 500, () -> {
                    throw new IllegalStateException("boom");
                }),
                source("snapshot", 200, () -> Optional.of(MinecraftVersion.parse("24w14a"))),
                source("good", 100, () -> Optional.of(MinecraftVersion.parse("1.21.1")))));
        assertEquals("good", result.source());
    }

    @Test
    void reportsEverySourceWhenAllFail() {
        VersionDetectionException error = assertThrows(VersionDetectionException.class, () -> VersionDetector.detect(List.of(
                source("second", 10, () -> {
                    throw new IllegalStateException("outer", new ClassNotFoundException("inner"));
                }),
                source("first", 20, Optional::empty),
                source("third", 0, () -> Optional.of(MinecraftVersion.parse("not-a-version"))))));
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
    void builtInSourcesRunAfterInstalledOnes() {
        assertEquals(List.of("version-json", "reflection"),
                VersionDetector.BUILT_IN.stream().map(VersionSource::name).toList());
        assertTrue(VersionDetector.BUILT_IN.get(0).priority() > VersionDetector.BUILT_IN.get(1).priority());
    }

    @Test
    void installRegistersReplacesAndCaches() {
        VersionDetector.reset();
        try {
            VersionDetector.install(source("custom", 1000, () -> Optional.of(MinecraftVersion.parse("26.1"))));
            assertEquals("26.1", VersionDetector.detect().toString());
            assertEquals("custom", VersionDetector.source());
            VersionDetector.install(source("custom", 1000, () -> Optional.of(MinecraftVersion.parse("1.21.1"))));
            assertEquals("1.21.1", VersionDetector.detect().toString());
        } finally {
            VersionDetector.reset();
        }
        assertEquals("none", VersionDetector.source());
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
            assertEquals(Optional.of(MinecraftVersion.parse("26.1.2")), new VersionJsonSource(loader).detect());
        }
    }

    @Test
    void snapshotVersionJsonFailsAndFallsThrough() {
        VersionDetector.Result result = VersionDetector.detect(List.of(
                source("version-json", 100, () -> Optional.of(MinecraftVersion.parse("24w14a"))),
                source("reflection", 0, () -> Optional.of(MinecraftVersion.parse("1.21.1")))));
        assertEquals("1.21.1", result.version().toString());
    }

    @Test
    void reflectionDoesNotApplyWithoutGameClasses() throws Exception {
        try (URLClassLoader loader = new URLClassLoader(new URL[0], null)) {
            assertEquals(Optional.empty(), new ReflectionSource(loader).detect());
        }
    }
}
