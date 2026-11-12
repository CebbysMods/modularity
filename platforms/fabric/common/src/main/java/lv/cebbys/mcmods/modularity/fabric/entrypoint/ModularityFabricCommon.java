package lv.cebbys.mcmods.modularity.fabric.entrypoint;

import lv.cebbys.mcmods.modularity.loom.entrypoint.ModularityLoomCommon;
import net.fabricmc.api.ModInitializer;

/** Fabric common entrypoint: installs the Fabric services and initializes every {@code "modularity"} entrypoint. */
public final class ModularityFabricCommon implements ModInitializer {
    @Override
    public void onInitialize() {
        ModularityLoomCommon.init("fabric");
    }
}
