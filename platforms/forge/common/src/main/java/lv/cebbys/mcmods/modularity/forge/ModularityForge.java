package lv.cebbys.mcmods.modularity.forge;

import lv.cebbys.mcmods.modularity.anvil.entrypoint.*;
import lv.cebbys.mcmods.modularity.anvil.ModularityAnvilServices;
import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.forge.entrypoint.ModularityForgeClient;
import lv.cebbys.mcmods.modularity.forge.entrypoint.ModularityForgeServer;
import lv.cebbys.mcmods.modularity.forge.loader.ForgeLoaderHooks;
import lv.cebbys.mcmods.modularity.forge.loader.ForgeLoaderInfo;
import lv.cebbys.mcmods.modularity.forge.loader.ForgeRenderData;
import lv.cebbys.mcmods.modularity.forge.loader.ForgeStorageBackend;
import lv.cebbys.mcmods.modularity.platform.EntryPoints;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.RegisterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forge entrypoint of Modularity. Forge constructs this class on both sides; the client and the dedicated server
 * entrypoints, selected by the distribution, run their own init. The constructor runs the detection and the launch
 * stage, and the common stage runs on the first {@link RegisterEvent}: Forge freezes the vanilla registries at the end
 * of mod construction and unfreezes them only for the {@code RegisterEvent} pass, so a value that is constructed
 * eagerly (a {@code Block}, for example) throws "Registry is already frozen" in {@code NewRegistryEvent}, which is
 * posted before {@code UNFREEZE_DATA}. The event fires after every mod is constructed, so a dependent mod can still
 * register itself in its own constructor.
 */
@Mod("modularity")
public final class ModularityForge {
    private static final Logger LOGGER = LoggerFactory.getLogger("modularity");
    private boolean commonRan;

    public ModularityForge(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        EntryPoints.active("mod-constructor", Loader.FORGE);
        ForgeLoaderHooks hooks = new ForgeLoaderHooks(modBus);
        ModularityAnvilServices services = new ModularityAnvilServices(new ForgeLoaderInfo(), new ForgeRenderData(),
                new ForgeStorageBackend(), hooks);
        ModularityAnvilLaunch.init(Loader.FORGE, services);
        modBus.addListener((RegisterEvent event) -> {
            if (commonRan) {
                return;
            }
            commonRan = true;
            ModularityAnvilLaunch.initMods(Loader.FORGE);
            ModularityAnvilCommon.init(Loader.FORGE, services);
            hooks.replay(event);
            // Forge posts its client registration events before its setup events, so the client stage runs here,
            // where the registries hold the values of the common stage (7.8).
            ModularityAnvilClient.runPending(Loader.FORGE);
        });
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModularityForgeClient.register(modBus);
        } else {
            ModularityForgeServer.register(modBus);
        }
        LOGGER.info("Forge platform loaded");
    }
}
