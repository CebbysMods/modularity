package lv.cebbys.mcmods.modularity.loom.client;

import lv.cebbys.mcmods.modularity.platform.compat.Ids;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/**
 * Runs an action (dropping cached OBJ models) when client resources reload. 1.21.11: registered with Fabric's resource loader v1.
 * 26.1: {@code registerReloadListener}.
 */
final class ObjModelReloadListener implements ResourceManagerReloadListener {
    private final Runnable action;

    private ObjModelReloadListener(Runnable action) {
        this.action = action;
    }

    static void register(Runnable action) {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(Ids.of("modularity", "obj_models"),
                new ObjModelReloadListener(action));
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        action.run();
    }
}
