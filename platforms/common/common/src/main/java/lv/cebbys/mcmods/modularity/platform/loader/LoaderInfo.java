package lv.cebbys.mcmods.modularity.platform.loader;

import lv.cebbys.mcmods.modularity.api.ModularityModCommon;
import lv.cebbys.mcmods.modularity.api.core.Side;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/** What the mod loader knows: the side, the loaded mods and the mods that declare Modularity entrypoints. */
public interface LoaderInfo {

    Side side();

    boolean isModLoaded(String modId);

    Optional<String> modVersion(String modId);

    boolean isDevelopment();

    /**
     * The mods built on Modularity, in load order: loader entrypoint order on Fabric, registration (mod
     * construction) order on NeoForge and Forge. The order is not the same on every loader.
     */
    List<ModEntry> modularMods();

    /** A mod id and how to create its entrypoint (may throw; the caller names the mod). */
    record ModEntry(String modId, Supplier<ModularityModCommon> mod) {
    }
}
