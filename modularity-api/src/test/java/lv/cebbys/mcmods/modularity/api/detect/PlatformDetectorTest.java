package lv.cebbys.mcmods.modularity.api.detect;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.api.core.PlatformVersion;
import lv.cebbys.mcmods.modularity.api.core.Side;
import lv.cebbys.mcmods.modularity.testing.allure.Functionality;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Epics;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Features;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Functionalities;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Issues;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Stories;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Tags;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The platform detector: marker classes, source priority and failure reports. */
@Epic(Epics.MODULARITY)
@Feature(Features.L1_UNIT)
@Story(Stories.PLATFORM_DETECTION)
@Issue(Issues.PR_4)
@Tag(Tags.PR_4)
class PlatformDetectorTest {
    private static final String QUILT = "org.quiltmc.loader.api.QuiltLoader";
    private static final String FABRIC = "net.fabricmc.loader.api.FabricLoader";
    private static final String NEOFORGE = "net.neoforged.fml.loading.FMLLoader";
    private static final String FORGE = "net.minecraftforge.fml.loading.FMLLoader";

    /** Sees only the given stub classes (from the test class path), nothing else. */
    private static final class StubLoader extends ClassLoader {
        private final Set<String> visible;

        StubLoader(String... visible) {
            super(null);
            this.visible = Set.of(visible);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (!visible.contains(name)) {
                throw new ClassNotFoundException(name);
            }
            String file = name.replace('.', '/') + ".class";
            try (InputStream in = PlatformDetectorTest.class.getClassLoader().getResourceAsStream(file)) {
                if (in == null) {
                    throw new ClassNotFoundException(name);
                }
                byte[] bytes = in.readAllBytes();
                return defineClass(name, bytes, 0, bytes.length);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }

    private interface Detect {
        Optional<DetectedPlatform> run() throws Exception;
    }

    private static PlatformSource source(String name, int priority, Detect detect) {
        return new PlatformSource() {
            @Override
            public int priority() {
                return priority;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public Optional<DetectedPlatform> detect() throws Exception {
                return detect.run();
            }
        };
    }

    @Test
    @Functionality(Functionalities.MARKER_CLASSES)
    @DisplayName("Each loader marker class maps to its loader")
    void eachMarkerIsRecognised() {
        assertMarker(Loader.QUILT, QUILT);
        assertMarker(Loader.FABRIC, FABRIC);
        assertMarker(Loader.NEOFORGE, NEOFORGE);
        assertMarker(Loader.FORGE, FORGE);
    }

    @Test
    @Functionality(Functionalities.MARKER_PRECEDENCE)
    @DisplayName("Quilt wins over Fabric because Quilt also ships the Fabric marker")
    void quiltWinsOverFabricBecauseItShipsFabricClasses() {
        assertMarkerFrom(List.of(FABRIC, QUILT), Loader.QUILT);
    }

    @Test
    @Functionality(Functionalities.MARKER_PRECEDENCE)
    @DisplayName("Marker order is Quilt, Fabric, NeoForge, Forge")
    void markerOrderIsQuiltFabricNeoForgeForge() {
        assertMarkerFrom(List.of(FABRIC, NEOFORGE, FORGE), Loader.FABRIC);
        assertMarkerFrom(List.of(NEOFORGE, FORGE), Loader.NEOFORGE);
        assertMarkerOrder();
    }

    @Test
    @Functionality(Functionalities.MARKER_CLASSES)
    @DisplayName("A class path without marker classes is not applicable")
    void noMarkerIsNotApplicable() {
        assertEquals(Optional.empty(), detectMarkers(List.of()), "no marker class gives no platform");
    }

    @Test
    @Functionality(Functionalities.SOURCE_ORDERING)
    @DisplayName("The source with the highest priority wins")
    void highestPrioritySourceWins() {
        DetectedPlatform platform = detect(
                source("markers", 0, () -> Optional.of(DetectedPlatform.of(Loader.FABRIC, PlatformVersion.unknown(),
                        Optional.empty(), false, "markers"))),
                source("loader", 1000, () -> Optional.of(DetectedPlatform.of(Loader.FABRIC,
                        PlatformVersion.parse("0.16.9"), Side.CLIENT, true, "loader"))));
        assertSource(platform, "loader");
        assertText(platform, "FABRIC 0.16.9, CLIENT (development)");
        assertClientSide(platform);
    }

    @Test
    @Functionality(Functionalities.SOURCE_ORDERING)
    @DisplayName("Detection skips an empty source and a source that throws")
    void fallsThroughEmptyAndFailingSources() {
        DetectedPlatform platform = detect(
                source("empty", 1000, Optional::empty),
                source("broken", 500, () -> {
                    throw new IllegalStateException("boom");
                }),
                source("markers", 0, () -> Optional.of(DetectedPlatform.of(Loader.FORGE, PlatformVersion.unknown(),
                        Optional.empty(), false, "markers"))));
        assertLoader(platform, Loader.FORGE);
        assertText(platform, "FORGE unknown, UNKNOWN");
    }

    @Test
    @Functionality(Functionalities.SOURCE_FAILURES)
    @DisplayName("A failed detection lists every source in priority order")
    void reportsEverySourceWhenAllFail() {
        assertAllSourcesReported(
                List.of("first: not applicable", "second: IllegalStateException: outer -> LinkageError: inner"),
                source("second", 10, () -> {
                    throw new IllegalStateException("outer", new LinkageError("inner"));
                }),
                source("first", 20, Optional::empty));
    }

    @Test
    @Functionality(Functionalities.SOURCE_FAILURES)
    @DisplayName("A detection without sources reports the misconfiguration")
    void noSourcesIsReported() {
        assertNoSourcesReported();
    }

    @Test
    @Functionality(Functionalities.BUILT_IN_SOURCES)
    @DisplayName("The built-in platform source reads marker classes")
    void builtInSourceIsMarkerClasses() {
        assertBuiltInSourceName();
    }

    @Test
    @Functionality(Functionalities.SOURCE_INSTALLATION)
    @DisplayName("An installed source replaces the built-in source and is cached")
    void installRegistersAndCaches() {
        PlatformDetector.reset();
        try {
            installCustomSource();
            assertInstalledSourceIsUsed();
        } finally {
            PlatformDetector.reset();
        }
        assertEquals("none", PlatformDetector.source(), "a reset removes the installed source");
    }

    @Step("Read the loader when the class path shows {0}")
    private Loader markers(List<String> visible) {
        return new MarkerClassSource(new StubLoader(visible.toArray(new String[0])))
                .detect().orElseThrow().loader();
    }

    @Step("Detect the platform when the class path shows {0}")
    private Optional<DetectedPlatform> detectMarkers(List<String> visible) {
        return new MarkerClassSource(new StubLoader(visible.toArray(new String[0]))).detect();
    }

    @Step("Check that the marker class {0} gives the loader {1}")
    private void assertMarker(Loader expected, String marker) {
        assertEquals(expected, markers(List.of(marker)));
    }

    @Step("Check the loader when the class path shows {0}")
    private void assertMarkerFrom(List<String> visible, Loader expected) {
        assertEquals(expected, markers(visible));
    }

    @Step("Check that the built-in marker order is Quilt, Fabric, NeoForge, Forge")
    private void assertMarkerOrder() {
        assertEquals(List.of(Loader.QUILT, Loader.FABRIC, Loader.NEOFORGE, Loader.FORGE),
                MarkerClassSource.MARKERS.stream().map(Map.Entry::getValue).toList());
    }

    @Step("Detect the platform from all sources, in priority order")
    private DetectedPlatform detect(PlatformSource... sources) {
        return PlatformDetector.detect(List.of(sources));
    }

    @Step("Check the client side and the loader version 0.16.9")
    private void assertClientSide(DetectedPlatform platform) {
        assertEquals(Optional.of(Side.CLIENT), platform.side(), "the side is the client");
        assertEquals("0.16.9", platform.loaderVersion().raw(), "the loader version is 0.16.9");
        assertTrue(platform.loaderVersion().isKnown(), "the loader version is known");
    }

    @Step("Detect without sources and check that the message names the missing sources")
    private void assertNoSourcesReported() {
        PlatformDetectionException error = assertThrows(PlatformDetectionException.class,
                () -> PlatformDetector.detect(List.of()));
        assertTrue(error.getMessage().contains("no platform sources"));
    }

    @Step("Check that the built-in platform source is the marker class source")
    private void assertBuiltInSourceName() {
        assertEquals(List.of("marker-classes"),
                PlatformDetector.BUILT_IN.stream().map(PlatformSource::name).toList());
    }

    @Step("Check that the source is \"{1}\"")
    private void assertSource(DetectedPlatform platform, String source) {
        assertEquals(source, platform.source());
    }

    @Step("Check that the loader is {1}")
    private void assertLoader(DetectedPlatform platform, Loader loader) {
        assertEquals(loader, platform.loader());
    }

    @Step("Check that the text form is \"{1}\"")
    private void assertText(DetectedPlatform platform, String text) {
        assertEquals(text, platform.toString());
    }

    @Step("Detect the platform when every source fails and check the failure list")
    private void assertAllSourcesReported(List<String> expected, PlatformSource... sources) {
        PlatformDetectionException error = assertThrows(PlatformDetectionException.class,
                () -> PlatformDetector.detect(List.of(sources)));
        assertEquals(expected, error.failures());
        assertTrue(error.getMessage().contains(PlatformDetectionException.ISSUES), "the message lists the issues");
    }

    @Step("Install a custom source with priority 1000 that reports Quilt 0.30.2 on the server")
    private void installCustomSource() {
        PlatformDetector.install(source("custom", 1000, () -> Optional.of(DetectedPlatform.of(Loader.QUILT,
                PlatformVersion.parse("0.30.2"), Side.SERVER, false, "custom"))));
    }

    @Step("Detect with the installed source and check that it is used")
    private void assertInstalledSourceIsUsed() {
        DetectedPlatform platform = PlatformDetector.detect();
        assertEquals(Loader.QUILT, platform.loader());
        assertEquals("custom", PlatformDetector.source());
        assertEquals(Optional.of(Side.SERVER), platform.side());
    }
}
