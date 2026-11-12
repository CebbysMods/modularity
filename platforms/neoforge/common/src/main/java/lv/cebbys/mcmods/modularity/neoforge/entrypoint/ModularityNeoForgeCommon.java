package lv.cebbys.mcmods.modularity.neoforge.entrypoint;

import lv.cebbys.mcmods.modularity.anvil.entrypoint.*;
import lv.cebbys.mcmods.modularity.anvil.ModularityAnvilServices;
import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.neoforge.loader.NeoForgeLoaderHooks;
import lv.cebbys.mcmods.modularity.neoforge.loader.NeoForgeLoaderInfo;
import lv.cebbys.mcmods.modularity.neoforge.loader.NeoForgeRenderData;
import lv.cebbys.mcmods.modularity.neoforge.loader.NeoForgeStorageBackend;
import lv.cebbys.mcmods.modularity.platform.EntryPoints;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common NeoForge entrypoint of Modularity. NeoForge constructs this class on both sides; the client and server
 * classes, selected by {@code dist}, run their own init. The constructor runs the detection and the launch stage, and
 * the common stage runs on the first {@link RegisterEvent}: NeoForge freezes the vanilla registries at the end of mod
 * construction and unfreezes them only for the {@code RegisterEvent} pass, so a value that is constructed eagerly (a
 * {@code Block}, for example) throws "Registry is already frozen" in {@code NewRegistryEvent}, which is posted before
 * {@code GameData.unfreezeData}. The event fires after every mod is constructed, so a dependent mod can still register
 * itself in its own constructor.
 */
@Mod("modularity")
public final class ModularityNeoForgeCommon {
    private static final Logger LOGGER = LoggerFactory.getLogger("modularity");
    private boolean commonRan;

    public ModularityNeoForgeCommon(IEventBus modBus) {
        EntryPoints.active("mod-constructor", Loader.NEOFORGE);
        NeoForgeLoaderHooks hooks = new NeoForgeLoaderHooks(modBus);
        ModularityAnvilServices services = new ModularityAnvilServices(new NeoForgeLoaderInfo(), new NeoForgeRenderData(),
                new NeoForgeStorageBackend(modBus), hooks);
        ModularityAnvilLaunch.init(Loader.NEOFORGE, services);
        modBus.addListener((RegisterEvent event) -> {
            if (commonRan) {
                return;
            }
            commonRan = true;
            ModularityAnvilLaunch.initMods(Loader.NEOFORGE);
            ModularityAnvilCommon.init(Loader.NEOFORGE, services);
            hooks.replay(event);
            // NeoForge posts its client registration events before its setup events, so the client stage runs here,
            // where the registries hold the values of the common stage (7.8).
            ModularityAnvilClient.runPending(Loader.NEOFORGE);
        });
        LOGGER.info("NeoForge platform loaded");
    }
}
