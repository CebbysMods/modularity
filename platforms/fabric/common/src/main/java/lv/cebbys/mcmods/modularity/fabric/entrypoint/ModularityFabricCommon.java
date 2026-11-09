package lv.cebbys.mcmods.modularity.fabric.entrypoint;

import lv.cebbys.mcmods.modularity.loom.entrypoint.LoomEntrypoints;
import net.fabricmc.api.ModInitializer;

/** Fabric common entrypoint: installs the Fabric seams and initializes every {@code "modularity"} entrypoint. */
public final class ModularityFabricCommon implements ModInitializer {
    @Override
    public void onInitialize() {
        LoomEntrypoints.common("fabric");
    }
}
