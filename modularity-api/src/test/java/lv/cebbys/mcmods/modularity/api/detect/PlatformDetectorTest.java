package lv.cebbys.mcmods.modularity.api.detect;

import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.api.core.PlatformVersion;
import lv.cebbys.mcmods.modularity.api.core.Side;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    private static Loader markers(String... visible) {
        return new MarkerClassSource(new StubLoader(visible)).detect().orElseThrow().loader();
    }

    @Test
    void eachMarkerIsRecognised() {
        assertEquals(Loader.QUILT, markers(QUILT));
        assertEquals(Loader.FABRIC, markers(FABRIC));
        assertEquals(Loader.NEOFORGE, markers(NEOFORGE));
        assertEquals(Loader.FORGE, markers(FORGE));
    }

    @Test
    void quiltWinsOverFabricBecauseItShipsFabricClasses() {
        assertEquals(Loader.QUILT, markers(FABRIC, QUILT));
    }

    @Test
    void markerOrderIsQuiltFabricNeoForgeForge() {
        assertEquals(Loader.FABRIC, markers(FABRIC, NEOFORGE, FORGE));
        assertEquals(Loader.NEOFORGE, markers(NEOFORGE, FORGE));
        assertEquals(List.of(Loader.QUILT, Loader.FABRIC, Loader.NEOFORGE, Loader.FORGE),
                MarkerClassSource.MARKERS.stream().map(Map.Entry::getValue).toList());
    }

    @Test
    void noMarkerIsNotApplicable() {
        assertEquals(Optional.empty(), new MarkerClassSource(new StubLoader()).detect());
    }

    @Test
    void highestPrioritySourceWins() {
        DetectedPlatform platform = PlatformDetector.detect(List.of(
                source("markers", 0, () -> Optional.of(DetectedPlatform.of(Loader.FABRIC, PlatformVersion.unknown(),
                        Optional.empty(), false, "markers"))),
                source("loader", 1000, () -> Optional.of(DetectedPlatform.of(Loader.FABRIC,
                        PlatformVersion.parse("0.16.9"), Side.CLIENT, true, "loader")))));
        assertEquals("loader", platform.source());
        assertEquals("FABRIC 0.16.9, CLIENT (development)", platform.toString());
        assertEquals(Optional.of(Side.CLIENT), platform.side());
        assertEquals("0.16.9", platform.loaderVersion().raw());
        assertTrue(platform.loaderVersion().isKnown());
    }

    @Test
    void fallsThroughEmptyAndFailingSources() {
        DetectedPlatform platform = PlatformDetector.detect(List.of(
                source("empty", 1000, Optional::empty),
                source("broken", 500, () -> {
                    throw new IllegalStateException("boom");
                }),
                source("markers", 0, () -> Optional.of(DetectedPlatform.of(Loader.FORGE, PlatformVersion.unknown(),
                        Optional.empty(), false, "markers")))));
        assertEquals(Loader.FORGE, platform.loader());
        assertEquals("FORGE unknown, UNKNOWN", platform.toString());
    }

    @Test
    void reportsEverySourceWhenAllFail() {
        PlatformDetectionException error = assertThrows(PlatformDetectionException.class, () -> PlatformDetector.detect(List.of(
                source("second", 10, () -> {
                    throw new IllegalStateException("outer", new LinkageError("inner"));
                }),
                source("first", 20, Optional::empty))));
        assertEquals(List.of("first: not applicable", "second: IllegalStateException: outer -> LinkageError: inner"),
                error.failures());
        assertTrue(error.getMessage().contains(PlatformDetectionException.ISSUES));
    }

    @Test
    void noSourcesIsReported() {
        PlatformDetectionException error = assertThrows(PlatformDetectionException.class,
                () -> PlatformDetector.detect(List.of()));
        assertTrue(error.getMessage().contains("no platform sources"));
    }

    @Test
    void builtInSourceIsMarkerClasses() {
        assertEquals(List.of("marker-classes"), PlatformDetector.BUILT_IN.stream().map(PlatformSource::name).toList());
    }

    @Test
    void installRegistersAndCaches() {
        PlatformDetector.reset();
        try {
            PlatformDetector.install(source("custom", 1000, () -> Optional.of(DetectedPlatform.of(Loader.QUILT,
                    PlatformVersion.parse("0.30.2"), Side.SERVER, false, "custom"))));
            DetectedPlatform platform = PlatformDetector.detect();
            assertEquals(Loader.QUILT, platform.loader());
            assertEquals("custom", PlatformDetector.source());
            assertEquals(Optional.of(Side.SERVER), platform.side());
        } finally {
            PlatformDetector.reset();
        }
        assertEquals("none", PlatformDetector.source());
    }
}
