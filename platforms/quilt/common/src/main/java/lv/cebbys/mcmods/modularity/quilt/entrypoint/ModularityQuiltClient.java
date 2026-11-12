package lv.cebbys.mcmods.modularity.quilt.entrypoint;

import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.loom.entrypoint.ModularityLoomClient;
import net.fabricmc.api.ClientModInitializer;

/** Quilt client entrypoint: installs the Loom client hooks and runs the client init. */
public final class ModularityQuiltClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModularityLoomClient.init(Loader.QUILT);
    }
}
