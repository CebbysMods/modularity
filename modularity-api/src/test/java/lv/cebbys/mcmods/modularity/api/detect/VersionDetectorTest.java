package lv.cebbys.mcmods.modularity.api.detect;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import lv.cebbys.mcmods.modularity.api.core.MinecraftVersion;
import lv.cebbys.mcmods.modularity.testing.allure.Functionality;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Epics;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Features;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Functionalities;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Issues;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Stories;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Tags;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The version detector: source priority, version.json, reflection and failure reports. */
@Epic(Epics.MODULARITY)
@Feature(Features.L1_UNIT)
@Story(Stories.VERSION_DETECTION)
@Issue(Issues.PR_4)
@Tag(Tags.PR_4)
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
    @Functionality(Functionalities.SOURCE_ORDERING)
    @DisplayName("The version source with the highest priority wins")
    void highestPrioritySourceWins() {
        VersionDetector.Result result = detect(
                source("low", 0, () -> Optional.of(MinecraftVersion.parse("1.20.1"))),
                source("high", 1000, () -> Optional.of(MinecraftVersion.parse("1.21.1"))),
                source("mid", 100, () -> Optional.of(MinecraftVersion.parse("1.19.4"))));
        assertVersion(result, "1.21.1");
        assertSource(result, "high");
    }

    @Test
    @Functionality(Functionalities.SOURCE_ORDERING)
    @DisplayName("Detection skips empty, failing and snapshot sources")
    void fallsThroughEmptyFailingAndUnparseableSources() {
        VersionDetector.Result result = detect(
                source("empty", 1000, Optional::empty),
                source("broken", 500, () -> {
                    throw new IllegalStateException("boom");
                }),
                source("snapshot", 200, () -> Optional.of(MinecraftVersion.parse("24w14a"))),
                source("good", 100, () -> Optional.of(MinecraftVersion.parse("1.21.1"))));
        assertSource(result, "good");
    }

    @Test
    @Functionality(Functionalities.SOURCE_FAILURES)
    @DisplayName("A failed detection lists every version source")
    void reportsEverySourceWhenAllFail() {
        assertAllSourcesReported();
    }

    @Test
    @Functionality(Functionalities.SOURCE_FAILURES)
    @DisplayName("A detection without version sources reports the misconfiguration")
    void noSourcesIsReported() {
        assertNoSourcesReported();
    }

    @Test
    @Functionality(Functionalities.BUILT_IN_SOURCES)
    @DisplayName("version.json runs before reflection")
    void builtInSourcesRunAfterInstalledOnes() {
        assertBuiltInOrder();
    }

    @Test
    @Functionality(Functionalities.SOURCE_INSTALLATION)
    @DisplayName("An installed version source replaces the previous one and is cached")
    void installRegistersReplacesAndCaches() {
        VersionDetector.reset();
        try {
            installSource("26.1");
            assertInstalledVersion("26.1");
            installSource("1.21.1");
            assertInstalledVersion("1.21.1");
        } finally {
            VersionDetector.reset();
        }
        assertNoInstalledSource();
    }

    @Test
    @Functionality(Functionalities.VERSION_JSON)
    @DisplayName("The version id is read from version.json and other files are ignored")
    void versionJsonIdIsRead() {
        assertJsonId("{\n  \"id\": \"1.21.1\",\n  \"name\": \"1.21.1\",\n  \"world_version\": 3955,\n  \"protocol_version\": 767\n}");
        assertJsonIdIgnored("{\"id\": \"some-other-file\"}");
    }

    @Test
    @Functionality(Functionalities.VERSION_JSON)
    @DisplayName("version.json is found in a game directory on the class path")
    void versionJsonIsFoundOnTheClassPath(@TempDir Path dir) throws Exception {
        try (URLClassLoader loader = classPathWithTwoDirectories(dir)) {
            assertVersionJsonFound(loader);
        }
    }

    @Test
    @Functionality(Functionalities.VERSION_PARSING)
    @DisplayName("A snapshot version.json is rejected and the next source answers")
    void snapshotVersionJsonFailsAndFallsThrough() {
        VersionDetector.Result result = detect(
                source("version-json", 100, () -> Optional.of(MinecraftVersion.parse("24w14a"))),
                source("reflection", 0, () -> Optional.of(MinecraftVersion.parse("1.21.1"))));
        assertVersion(result, "1.21.1");
    }

    @Test
    @Functionality(Functionalities.REFLECTION)
    @DisplayName("Reflection does not apply when the game classes are absent")
    void reflectionDoesNotApplyWithoutGameClasses() throws Exception {
        try (URLClassLoader loader = new URLClassLoader(new URL[0], null)) {
            assertReflectionNotApplicable(loader);
        }
    }

    @Step("Detect the Minecraft version from all sources, in priority order")
    private VersionDetector.Result detect(VersionSource... sources) {
        return VersionDetector.detect(List.of(sources));
    }

    @Step("Check that the built-in sources are version.json and reflection, in that order")
    private void assertBuiltInOrder() {
        assertEquals(List.of("version-json", "reflection"),
                VersionDetector.BUILT_IN.stream().map(VersionSource::name).toList());
        assertTrue(VersionDetector.BUILT_IN.get(0).priority() > VersionDetector.BUILT_IN.get(1).priority(),
                "version.json has the higher priority");
    }

    @Step("Check that the source name is \"none\" after the reset")
    private void assertNoInstalledSource() {
        assertEquals("none", VersionDetector.source());
    }

    @Step("Detect without sources and check that the message names the missing sources")
    private void assertNoSourcesReported() {
        VersionDetectionException error = assertThrows(VersionDetectionException.class,
                () -> VersionDetector.detect(List.of()));
        assertTrue(error.getMessage().contains("no version sources"));
    }

    @Step("Read the version id from the JSON text and expect 1.21.1")
    private void assertJsonId(String json) {
        assertEquals(Optional.of("1.21.1"), VersionJsonSource.parseId(json));
    }

    @Step("Read the version id from a JSON text that is not a game version and expect no id")
    private void assertJsonIdIgnored(String json) {
        assertEquals(Optional.empty(), VersionJsonSource.parseId(json));
    }

    @Step("Detect the version from version.json on the class path and expect 26.1.2")
    private void assertVersionJsonFound(URLClassLoader loader) throws Exception {
        assertEquals(Optional.of(MinecraftVersion.parse("26.1.2")), new VersionJsonSource(loader).detect());
    }

    @Step("Run the reflection source with no game classes and expect no version")
    private void assertReflectionNotApplicable(URLClassLoader loader) throws Exception {
        assertEquals(Optional.empty(), new ReflectionSource(loader).detect());
    }

    @Step("Check that the version is \"{1}\"")
    private void assertVersion(VersionDetector.Result result, String version) {
        assertEquals(version, result.version().toString());
    }

    @Step("Check that the source is \"{1}\"")
    private void assertSource(VersionDetector.Result result, String source) {
        assertEquals(source, result.source());
    }

    @Step("Detect the version when an empty source, a failing source and an unparseable source all fail")
    private void assertAllSourcesReported() {
        VersionDetectionException error = assertThrows(VersionDetectionException.class,
                () -> VersionDetector.detect(List.of(
                        source("second", 10, () -> {
                            throw new IllegalStateException("outer", new ClassNotFoundException("inner"));
                        }),
                        source("first", 20, Optional::empty),
                        source("third", 0, () -> Optional.of(MinecraftVersion.parse("not-a-version"))))));
        assertEquals(3, error.failures().size(), "one failure per source");
        assertEquals("first: not applicable", error.failures().get(0), "the empty source comes first");
        assertEquals("second: IllegalStateException: outer -> ClassNotFoundException: inner",
                error.failures().get(1), "the failing source comes second");
        assertTrue(error.failures().get(2).startsWith("third: IllegalArgumentException"),
                "the unparseable version comes third");
        assertTrue(error.getMessage().contains(VersionDetectionException.ISSUES), "the message lists the issues");
    }

    @Step("Install a custom source with priority 1000 that reports \"{0}\"")
    private void installSource(String version) {
        VersionDetector.install(source("custom", 1000, () -> Optional.of(MinecraftVersion.parse(version))));
    }

    @Step("Detect with the installed source and check the version \"{0}\"")
    private void assertInstalledVersion(String version) {
        assertEquals(version, VersionDetector.detect().toString());
        assertEquals("custom", VersionDetector.source());
    }

    @Step("Write version.json into a game directory and an unrelated version.json into another directory")
    private URLClassLoader classPathWithTwoDirectories(Path dir) throws IOException {
        Path other = Files.createDirectories(dir.resolve("other"));
        Files.writeString(other.resolve("version.json"), "{\"id\": \"unrelated\"}");
        Path game = Files.createDirectories(dir.resolve("game"));
        Files.writeString(game.resolve("version.json"), "{\"id\": \"26.1.2\", \"world_version\": 1}");
        return new URLClassLoader(new URL[]{other.toUri().toURL(), game.toUri().toURL()}, null);
    }
}
