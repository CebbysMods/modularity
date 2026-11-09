package lv.cebbys.mcmods.modularity.neoforge.entrypoint;

import lv.cebbys.mcmods.modularity.neoforge.loader.NeoForgeLoaderHooks;
import lv.cebbys.mcmods.modularity.neoforge.loader.NeoForgeLoaderInfo;
import lv.cebbys.mcmods.modularity.neoforge.loader.NeoForgeRenderData;
import lv.cebbys.mcmods.modularity.neoforge.loader.NeoForgeStorageBackend;
import lv.cebbys.mcmods.modularity.platform.EntryPoint;
import lv.cebbys.mcmods.modularity.platform.EntryPoints;
import lv.cebbys.mcmods.modularity.platform.ModularityCommon;
import lv.cebbys.mcmods.modularity.platform.compat.SyncedData;
import lv.cebbys.mcmods.modularity.platform.loader.Loaders;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common NeoForge entrypoint of Modularity. NeoForge constructs this class on both sides; the client and server
 * classes, selected by {@code dist}, run their own init. The constructor installs the seams and registers the
 * common setup listener. The init itself runs in {@code enqueueWork}, on the main thread after the parallel phase.
 */
@Mod("modularity")
public final class ModularityNeoForgeCommon {
    private static final Logger LOGGER = LoggerFactory.getLogger("modularity");

    public ModularityNeoForgeCommon(IEventBus modBus) {
        EntryPoints.active("mod-constructor", "neoforge");
        Loaders.install(new NeoForgeLoaderInfo(), new NeoForgeRenderData(),
                new NeoForgeStorageBackend(), new NeoForgeLoaderHooks());
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            EntryPoints.active(EntryPoint.COMMON, "neoforge");
            SyncedData.init();
            ModularityCommon.init("neoforge");
        }));
        LOGGER.info("[modularity] NeoForge platform loaded");
    }
}
