package lv.cebbys.mcmods.modularity.fabric.loader;

import lv.cebbys.mcmods.modularity.api.ModularMod;
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

    /** Mods with a {@code "modularity"} entrypoint, in load order. */
    List<ModEntry> modularMods();

    /** A mod id and how to create its entrypoint (may throw; the caller names the mod). */
    record ModEntry(String modId, Supplier<ModularMod> mod) {
    }
}
