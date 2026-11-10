package lv.cebbys.mcmods.modularity.api.detect;

import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.api.core.PlatformVersion;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Checks which loader's entry class is present, without loading it. Quilt comes before Fabric,
 * because Quilt also ships Fabric's classes. Returns empty when no marker is found, and never knows
 * the loader version or the side.
 */
public final class MarkerClassSource implements PlatformSource {
    /** Checked in this order. */
    static final List<Map.Entry<String, Loader>> MARKERS = List.of(
            Map.entry("org.quiltmc.loader.api.QuiltLoader", Loader.QUILT),
            Map.entry("net.fabricmc.loader.api.FabricLoader", Loader.FABRIC),
            Map.entry("net.neoforged.fml.loading.FMLLoader", Loader.NEOFORGE),
            Map.entry("net.minecraftforge.fml.loading.FMLLoader", Loader.FORGE));

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
        for (Map.Entry<String, Loader> marker : MARKERS) {
            if (exists(marker.getKey())) {
                return Optional.of(DetectedPlatform.of(marker.getValue(), PlatformVersion.unknown(),
                        Optional.empty(), false, name()));
            }
        }
        return Optional.empty();
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
