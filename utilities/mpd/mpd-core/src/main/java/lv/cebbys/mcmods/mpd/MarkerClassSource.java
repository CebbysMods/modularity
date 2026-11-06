package lv.cebbys.mcmods.mpd;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Checks which loader's entry class is present, without loading it. Quilt comes before Fabric,
 * because Quilt also ships Fabric's classes. Reports {@link Platform#UNKNOWN} when none is found,
 * and never knows the loader version or side.
 */
public final class MarkerClassSource implements PlatformSource {
    /** Checked in this order. */
    static final List<Map.Entry<String, Platform>> MARKERS = List.of(
            Map.entry("org.quiltmc.loader.api.QuiltLoader", Platform.QUILT),
            Map.entry("net.fabricmc.loader.api.FabricLoader", Platform.FABRIC),
            Map.entry("net.neoforged.fml.loading.FMLLoader", Platform.NEOFORGE),
            Map.entry("net.minecraftforge.fml.loading.FMLLoader", Platform.FORGE));

    private final ClassLoader loader;

    public MarkerClassSource() {
        this(MarkerClassSource.class.getClassLoader());
    }

    MarkerClassSource(ClassLoader loader) {
        this.loader = loader;
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public String name() {
        return "marker-classes";
    }

    @Override
    public Optional<DetectedPlatform> detect() {
        Platform found = Platform.UNKNOWN;
        for (Map.Entry<String, Platform> marker : MARKERS) {
            if (exists(marker.getKey())) {
                found = marker.getValue();
                break;
            }
        }
        return Optional.of(DetectedPlatform.of(found, DetectedPlatform.UNKNOWN_VERSION, Side.UNKNOWN, false, name()));
    }

    private boolean exists(String className) {
        try {
            Class.forName(className, false, loader);
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}
