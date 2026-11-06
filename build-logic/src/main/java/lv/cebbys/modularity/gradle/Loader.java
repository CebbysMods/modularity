package lv.cebbys.modularity.gradle;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Mod loader of a platform. Each loader has a stack of layer roots under {@code platforms/}, lowest first: the
 * Minecraft code every loader shares, then the loader's own code (docs/07, 7.3).
 */
public enum Loader {
    FABRIC(List.of("minecraft", "fabric"), true),
    /** Runs the Fabric layers on Quilt Loader; Quilt-only differences go to {@code platforms/quilt}. */
    QUILT(List.of("minecraft", "fabric", "quilt"), false),
    NEOFORGE(List.of("minecraft", "neoforge"), true),
    FORGE(List.of("minecraft", "forge"), true);

    private final List<String> layerRoots;
    private final boolean publishes;

    Loader(List<String> layerRoots, boolean publishes) {
        this.layerRoots = layerRoots;
        this.publishes = publishes;
    }

    /** {@code fabric}, {@code quilt}, {@code neoforge}, {@code forge}: table value, project and folder names. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Folders under {@code platforms/} whose layers make up this loader's sources, lowest first. */
    public List<String> layerRoots() {
        return layerRoots;
    }

    /**
     * Whether the platform has its own jar in {@code collectJars}. Quilt platforms run the Fabric jar's code on Quilt
     * Loader: they verify that the Fabric jar works there and ship nothing of their own.
     */
    public boolean publishes() {
        return publishes;
    }

    public static Loader parse(String id) {
        return Arrays.stream(values()).filter(l -> l.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown loader '" + id + "' (one of "
                        + Arrays.stream(values()).map(Loader::id).toList() + ")"));
    }
}
