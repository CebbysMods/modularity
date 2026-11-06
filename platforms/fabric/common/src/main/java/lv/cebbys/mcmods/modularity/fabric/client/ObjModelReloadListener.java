package lv.cebbys.mcmods.modularity.fabric.client;

import lv.cebbys.mcmods.modularity.fabric.compat.Ids;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

/** Runs an action (dropping cached OBJ models) when client resources reload. */
final class ObjModelReloadListener implements SimpleSynchronousResourceReloadListener {
    private final Runnable action;

    private ObjModelReloadListener(Runnable action) {
        this.action = action;
    }

    static void register(Runnable action) {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new ObjModelReloadListener(action));
    }

    @Override
    public ResourceLocation getFabricId() {
        return Ids.of("modularity", "obj_models");
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        action.run();
    }
}
