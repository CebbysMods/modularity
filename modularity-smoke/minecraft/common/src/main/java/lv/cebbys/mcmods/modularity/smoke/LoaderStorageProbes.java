package lv.cebbys.mcmods.modularity.smoke;

import lv.cebbys.mcmods.modularity.platform.entity.ModularEntity;

/**
 * Reads an entity tank through the lookup of the running loader, which is the path another mod takes (docs/07 7.6).
 *
 * <p>Each loader publishes its own lookup: Fabric an {@code EntityApiLookup}, NeoForge and Forge an entity capability.
 * The smoke sources of the {@code minecraft} layer compile for every loader, so the loader root of the smoke mod
 * installs the one the run has, and the shared smoke calls it.</p>
 */
public final class LoaderStorageProbes {
    /** The lookup of one loader: stores fluid in an entity tank and reports the millibuckets it accepted. */
    @FunctionalInterface
    public interface Probe {
        /** @return the millibuckets stored, or {@code -1} when the lookup finds no tank */
        long storeThroughLookup(ModularEntity entity, int millibuckets);
    }

    private static Probe probe;

    private LoaderStorageProbes() {
    }

    /** Installs the probe of the running loader; the smoke client calls it on the client only. */
    public static void install(Probe installed) {
        probe = installed;
    }

    /** Stores {@code millibuckets} of water through the loader lookup; {@code -1} when no lookup finds a tank. */
    public static long store(ModularEntity entity, int millibuckets) {
        Probe installed = probe;
        return installed == null ? -1L : installed.storeThroughLookup(entity, millibuckets);
    }
}
