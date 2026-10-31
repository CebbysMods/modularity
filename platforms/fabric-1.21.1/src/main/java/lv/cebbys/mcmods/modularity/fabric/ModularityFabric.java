package lv.cebbys.mcmods.modularity.fabric;

import lv.cebbys.mcmods.modularity.api.Modularity;
import lv.cebbys.mcmods.modularity.api.ModularMod;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Fabric entrypoint: installs the runtime and initializes every {@code "modularity"} entrypoint. */
public final class ModularityFabric implements ModInitializer {
    public static final String ENTRYPOINT = "modularity";
    private static final Logger LOGGER = LoggerFactory.getLogger("modularity");
    private static final List<LoadedMod> MODS = new ArrayList<>();

    /** A mod initialized by Modularity; the same instance receives the client init. */
    public record LoadedMod(String modId, ModularMod mod) {
    }

    public static List<LoadedMod> mods() {
        return Collections.unmodifiableList(MODS);
    }

    @Override
    public void onInitialize() {
        Modularity.install(FabricRuntime.INSTANCE);
        var containers = FabricLoader.getInstance().getEntrypointContainers(ENTRYPOINT, ModularMod.class);
        LOGGER.info("Modularity {} on Fabric, Minecraft {}: initializing {} mod(s)",
                FabricRuntime.INSTANCE.modVersion("modularity").orElse("?"),
                FabricRuntime.INSTANCE.minecraftVersion(), containers.size());
        for (var container : containers) {
            String modId = container.getProvider().getMetadata().getId();
            try {
                ModularMod mod = container.getEntrypoint();
                mod.init(new FabricModContext(modId));
                MODS.add(new LoadedMod(modId, mod));
            } catch (RuntimeException e) {
                throw new IllegalStateException("Modularity mod '" + modId + "' failed to initialize", e);
            }
        }
    }
}
