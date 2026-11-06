package lv.cebbys.mcmods.modularity.fabric;

import lv.cebbys.mcmods.modularity.api.ModularMod;
import lv.cebbys.mcmods.modularity.api.Modularity;
import lv.cebbys.mcmods.modularity.fabric.compat.SyncedData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/** Fabric entrypoint: installs the runtime and initializes every {@code "modularity"} entrypoint. */
public final class ModularityFabric implements ModInitializer {
    public static final String ENTRYPOINT = "modularity";
    private static final Logger LOGGER = LoggerFactory.getLogger("modularity");
    private static final List<LoadedMod> MODS = new ArrayList<>();

    /** A mod initialized by Modularity; the same instance receives the client/server init. */
    public record LoadedMod(String modId, ModularMod mod) {
    }

    public static List<LoadedMod> mods() {
        return Collections.unmodifiableList(MODS);
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

    @Override
    public void onInitialize() {
        SyncedData.init();
        Modularity.install(FabricRuntime.INSTANCE);
        var containers = FabricLoader.getInstance().getEntrypointContainers(ENTRYPOINT, ModularMod.class);
        LOGGER.info("Modularity {} on Fabric, Minecraft {}: initializing {} mod(s)",
                FabricRuntime.INSTANCE.modVersion("modularity").orElse("?"),
                FabricRuntime.INSTANCE.minecraftVersion(), containers.size());
        for (var container : containers) {
            String modId = container.getProvider().getMetadata().getId();
            run(modId, "to initialize", () -> {
                ModularMod mod = container.getEntrypoint();
                mod.initCommon(new FabricModContext(modId));
                MODS.add(new LoadedMod(modId, mod));
            });
        }
    }
}
