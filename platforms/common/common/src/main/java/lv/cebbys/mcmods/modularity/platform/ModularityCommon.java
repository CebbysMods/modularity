package lv.cebbys.mcmods.modularity.platform;

import lv.cebbys.mcmods.modularity.api.ModularMod;
import lv.cebbys.mcmods.modularity.api.ModularModRegistry;
import lv.cebbys.mcmods.modularity.api.Modularity;
import lv.cebbys.mcmods.modularity.platform.loader.LoaderInfo;
import lv.cebbys.mcmods.modularity.platform.loader.Loaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/** Initializes the Modularity mods; the loader's entrypoint installs the seams ({@link Loaders}) first. */
public final class ModularityCommon {
    private static final Logger LOGGER = LoggerFactory.getLogger("modularity");
    private static final List<LoadedMod> MODS = new ArrayList<>();

    /** A mod initialized by Modularity; the same instance receives the client/server init. */
    public record LoadedMod(String modId, ModularMod mod) {
    }

    private ModularityCommon() {
    }

    public static List<LoadedMod> mods() {
        return Collections.unmodifiableList(MODS);
    }

    /** Registers a mod discovered by a generated loader adapter; call it before common initialization. */
    public static void register(String modId, ModularMod mod) {
        ModularModRegistry.register(modId, mod);
    }

    /** Installs the runtime and runs {@code initCommon} of every mod with a Modularity entrypoint. */
    public static void init(String loaderName) {
        ModularModRegistry.freeze();
        Modularity.install(MinecraftRuntime.INSTANCE);
        List<LoaderInfo.ModEntry> entries = Loaders.info().modularMods();
        LOGGER.info("Modularity {} on {}, Minecraft {}: initializing {} mod(s)",
                MinecraftRuntime.INSTANCE.modVersion("modularity").orElse("?"), loaderName,
                MinecraftRuntime.INSTANCE.minecraftVersion(), entries.size());
        for (LoaderInfo.ModEntry entry : entries) {
            run(entry.modId(), "to initialize", () -> {
                ModularMod mod = entry.mod().get();
                mod.initCommon(new MinecraftModContext(entry.modId()));
                MODS.add(new LoadedMod(entry.modId(), mod));
            });
        }
    }

    /** Runs {@code init} for every loaded mod; a failure names the mod ("failed {@code what}"). */
    public static void forEachMod(String what, Consumer<LoadedMod> init) {
        for (LoadedMod loaded : MODS) {
            run(loaded.modId(), what, () -> init.accept(loaded));
        }
    }

    private static void run(String modId, String what, Runnable init) {
        try {
            init.run();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Modularity mod '" + modId + "' failed " + what, e);
        }
    }
}
