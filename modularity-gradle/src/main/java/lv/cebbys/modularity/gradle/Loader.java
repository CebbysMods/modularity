package lv.cebbys.modularity.gradle;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Mod loader of a platform. Each loader has a stack of layer roots under {@code platforms/}, lowest first: the
 * Minecraft code every loader shares, the code shared by every loader that is not Minecraft code, then the loader's
 * own code (docs/07, 7.3).
 */
public enum Loader {
    FABRIC("minecraft", "common", "loom", "fabric"),
    /** Runs the Fabric-compatibility layers on Quilt Loader; Quilt-only differences go to {@code platforms/quilt}. */
    QUILT("minecraft", "common", "loom", "quilt"),
    /** {@code anvil} is the code the two Forge-family loaders share; only types neither loader owns may live there. */
    NEOFORGE("minecraft", "common", "anvil", "neoforge"),
    FORGE("minecraft", "common", "anvil", "forge");

    private final List<String> layerRoots;

    Loader(String... layerRoots) {
        this.layerRoots = Stream.of(layerRoots).toList();
    }

    /** {@code fabric}, {@code quilt}, {@code neoforge}, {@code forge}: table value, project and folder names. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Folders under {@code platforms/} whose layers make up this loader's sources, lowest first. */
    public List<String> layerRoots() {
        return layerRoots;
    }

    public static Loader parse(String id) {
        return Arrays.stream(values()).filter(l -> l.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown loader '" + id + "' (one of "
                        + Arrays.stream(values()).map(Loader::id).toList() + ")"));
    }
}
