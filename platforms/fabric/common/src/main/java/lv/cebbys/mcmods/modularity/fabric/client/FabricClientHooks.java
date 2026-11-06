package lv.cebbys.mcmods.modularity.fabric.client;

import lv.cebbys.mcmods.modularity.api.client.DynamicModel;
import lv.cebbys.mcmods.modularity.api.client.HudLayer;
import lv.cebbys.mcmods.modularity.fabric.compat.KeyMappings;
import lv.cebbys.mcmods.modularity.fabric.entity.ModularEntity;
import lv.cebbys.mcmods.modularity.fabric.loader.ClientHooks;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** {@link ClientHooks} with Fabric API; dynamic block models go through Fabric's model loading plugin. */
public final class FabricClientHooks implements ClientHooks {

    @Override
    public KeyMapping keyMapping(String modId, String name, int defaultKey) {
        return KeyMappings.register(modId, name, defaultKey);
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }

    @Override
    public <P extends CustomPacketPayload> void onClientPayload(CustomPacketPayload.Type<P> type, Consumer<P> handler) {
        ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> handler.accept(payload));
    }

    @Override
    public void entityRenderer(EntityType<ModularEntity> type, EntityRendererProvider<ModularEntity> provider) {
        EntityRendererRegistry.register(type, provider);
    }

    @Override
    public void blockModels(Map<Block, DynamicModel> models) {
        ModelLoadingPlugin.register(plugin -> models.forEach((block, model) ->
                plugin.registerBlockStateResolver(block, context -> {
                    var unbaked = new DynamicUnbakedModel(model);
                    for (var state : block.getStateDefinition().getPossibleStates()) {
                        context.setModel(state, unbaked);
                    }
                })));
    }

    @Override
    public void hud(List<HudLayer> layers) {
        HudHook.install(layers);
    }

    @Override
    public void onResourceReload(Runnable action) {
        ObjModelReloadListener.register(action);
    }

    @Override
    public void onClientTick(Consumer<Minecraft> action) {
        ClientTickEvents.END_CLIENT_TICK.register(action::accept);
    }
}
