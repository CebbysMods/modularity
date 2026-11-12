package lv.cebbys.mcmods.modularity.quilt.entrypoint;

import lv.cebbys.mcmods.modularity.loom.entrypoint.ModularityLoomCommon;
import net.fabricmc.api.ModInitializer;

/** Quilt common entrypoint: installs the Loom seams and initializes every {@code "modularity"} entrypoint. */
public final class ModularityQuiltCommon implements ModInitializer {
    @Override
    public void onInitialize() {
        ModularityLoomCommon.init("quilt");
    }
}
