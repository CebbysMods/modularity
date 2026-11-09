package lv.cebbys.mcmods.modularity.fabric.entrypoint;

import lv.cebbys.mcmods.modularity.platform.EntryPoint;
import lv.cebbys.mcmods.modularity.platform.EntryPoints;
import lv.cebbys.mcmods.modularity.platform.ModularityCommon;
import lv.cebbys.mcmods.modularity.platform.compat.SyncedData;
import lv.cebbys.mcmods.modularity.fabric.loader.FabricLoaderHooks;
import lv.cebbys.mcmods.modularity.fabric.loader.FabricLoaderInfo;
import lv.cebbys.mcmods.modularity.fabric.loader.FabricLoaderName;
import lv.cebbys.mcmods.modularity.fabric.loader.FabricRenderData;
import lv.cebbys.mcmods.modularity.fabric.loader.FabricStorageBackend;
import lv.cebbys.mcmods.modularity.platform.loader.Loaders;
import net.fabricmc.api.ModInitializer;

/** Fabric common entrypoint: installs the Fabric seams and initializes every {@code "modularity"} entrypoint. */
public final class ModularityFabricCommon implements ModInitializer {
    @Override
    public void onInitialize() {
        EntryPoints.active(EntryPoint.COMMON, FabricLoaderName.get());
        Loaders.install(new FabricLoaderInfo(), new FabricRenderData(), new FabricStorageBackend(), new FabricLoaderHooks());
        SyncedData.init();
        ModularityCommon.init(FabricLoaderName.get());
    }
}
