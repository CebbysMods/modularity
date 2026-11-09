package lv.cebbys.mcmods.modularity.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The mods built on Modularity, in registration order.
 *
 * <p>A loader with a generated {@code @Mod} adapter registers the mod from the adapter constructor. The platform
 * freezes the registry when common initialization starts, so a late registration fails instead of being ignored.
 *
 * <p>This class lives in the API because the universal jar relocates the platform classes under
 * {@code universal.<loader>}. An adapter of another mod must link against unrelocated classes.
 */
public final class ModularModRegistry {
    private static final Map<String, ModularMod> MODS = new LinkedHashMap<>();
    private static boolean frozen;

    private ModularModRegistry() {
    }

    /** One registered mod and its mod id. */
    public record Entry(String modId, ModularMod mod) {
    }

    /** Registers {@code mod} under {@code modId}. Fails on a duplicate id or after {@link #freeze()}. */
    public static synchronized void register(String modId, ModularMod mod) {
        Objects.requireNonNull(modId, "modId");
        Objects.requireNonNull(mod, "mod");
        if (frozen) {
            throw new IllegalStateException(
                    "Modularity mod '" + modId + "' is registered after common initialization started");
        }
        if (MODS.putIfAbsent(modId, mod) != null) {
            throw new IllegalStateException("Modularity mod '" + modId + "' is already registered");
        }
    }

    /** Stops further registration; the platform calls this at the start of common initialization. */
    public static synchronized void freeze() {
        frozen = true;
    }

    /** The registered mods, in registration order. */
    public static synchronized List<Entry> entries() {
        return List.copyOf(MODS.entrySet().stream()
                .map(entry -> new Entry(entry.getKey(), entry.getValue()))
                .toList());
    }
}
