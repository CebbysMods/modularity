package lv.cebbys.mcmods.mpd;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
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

    private static Platform markers(String... visible) {
        return new MarkerClassSource(new StubLoader(visible)).detect().orElseThrow().platform();
    }

    @Test
    void eachMarkerIsRecognised() {
        assertEquals(Platform.QUILT, markers(QUILT));
        assertEquals(Platform.FABRIC, markers(FABRIC));
        assertEquals(Platform.NEOFORGE, markers(NEOFORGE));
        assertEquals(Platform.FORGE, markers(FORGE));
    }

    @Test
    void quiltWinsOverFabricBecauseItShipsFabricClasses() {
        assertEquals(Platform.QUILT, markers(FABRIC, QUILT));
    }

    @Test
    void markerOrderIsQuiltFabricNeoForgeForge() {
        assertEquals(Platform.FABRIC, markers(FABRIC, NEOFORGE, FORGE));
        assertEquals(Platform.NEOFORGE, markers(NEOFORGE, FORGE));
        assertEquals(List.of(Platform.QUILT, Platform.FABRIC, Platform.NEOFORGE, Platform.FORGE),
                MarkerClassSource.MARKERS.stream().map(java.util.Map.Entry::getValue).toList());
    }

    @Test
    void noMarkerIsUnknown() {
        DetectedPlatform platform = new MarkerClassSource(new StubLoader()).detect().orElseThrow();
        assertEquals(Platform.UNKNOWN, platform.platform());
        assertEquals(DetectedPlatform.UNKNOWN_VERSION, platform.platformVersion());
        assertEquals(Side.UNKNOWN, platform.side());
        assertEquals("marker-classes", platform.source());
    }

    @Test
    void highestPrioritySourceWins() {
        DetectedPlatform platform = PlatformDetector.detect(List.of(
                source("markers", 0, () -> Optional.of(DetectedPlatform.of(Platform.FABRIC, "unknown", Side.UNKNOWN, false, "markers"))),
                source("loader", 1000, () -> Optional.of(DetectedPlatform.of(Platform.FABRIC, "0.16.9", Side.CLIENT, true, "loader")))));
        assertEquals("loader", platform.source());
        assertEquals("FABRIC 0.16.9, CLIENT (development)", platform.toString());
    }

    @Test
    void fallsThroughEmptyAndFailingSources() {
        DetectedPlatform platform = PlatformDetector.detect(List.of(
                source("empty", 1000, Optional::empty),
                source("broken", 500, () -> {
                    throw new IllegalStateException("boom");
                }),
                source("markers", 0, () -> Optional.of(DetectedPlatform.of(Platform.FORGE, "unknown", Side.UNKNOWN, false, "markers")))));
        assertEquals(Platform.FORGE, platform.platform());
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
    void builtInSourceIsRegistered() {
        List<String> names = java.util.ServiceLoader.load(PlatformSource.class).stream()
                .map(provider -> provider.get().name()).toList();
        assertEquals(List.of("marker-classes"), names);
    }
}
