package lv.cebbys.mcmods.modularity.fabric;

import lv.cebbys.mcmods.modularity.fabric.compat.SyncedData;
import lv.cebbys.mcmods.modularity.fabric.loader.FabricLoaderInfo;
import lv.cebbys.mcmods.modularity.fabric.loader.FabricRenderData;
import lv.cebbys.mcmods.modularity.fabric.loader.Loaders;
import net.fabricmc.api.ModInitializer;

/** Fabric entrypoint: installs the Fabric seams and initializes every {@code "modularity"} entrypoint. */
public final class ModularityFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Loaders.install(new FabricLoaderInfo(), new FabricRenderData());
        SyncedData.init();
        ModularityCore.initCommon("Fabric", FabricModContext::new);
    }
}
