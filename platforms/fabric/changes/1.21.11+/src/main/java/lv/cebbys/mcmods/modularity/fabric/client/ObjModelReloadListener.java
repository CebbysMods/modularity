package lv.cebbys.mcmods.modularity.fabric.client;

import lv.cebbys.mcmods.modularity.fabric.compat.Ids;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/** Drops the cached OBJ models when client resources reload. 1.21.11: registered with Fabric's resource loader v1. */
final class ObjModelReloadListener implements ResourceManagerReloadListener {
    private ObjModelReloadListener() {
    }

    static void register() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(Ids.of("modularity", "obj_models"),
                new ObjModelReloadListener());
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        FabricModelRef.clearAll();
    }
}
