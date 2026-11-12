package lv.cebbys.mcmods.modularity.neoforge.client;

import lv.cebbys.mcmods.modularity.api.client.DynamicModel;
import lv.cebbys.mcmods.modularity.api.client.HudLayer;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.neoforge.network.NeoForgePayloads;
import lv.cebbys.mcmods.modularity.platform.client.McHudContext;
import lv.cebbys.mcmods.modularity.platform.client.RunnableReloadListener;
import lv.cebbys.mcmods.modularity.platform.Wrap;
import lv.cebbys.mcmods.modularity.platform.entity.ModularEntity;
import lv.cebbys.mcmods.modularity.platform.loader.ClientHooks;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The NeoForge client seam. The tick, the HUD and the payload handlers use the game bus; the key mappings, the entity
 * renderers, the reload listener and the dynamic block models use the mod bus, because their events are mod-bus
 * events.
 */
public final class NeoForgeClientHooks implements ClientHooks {
    private final IEventBus modBus;

    public NeoForgeClientHooks(IEventBus modBus) {
        this.modBus = modBus;
    }

    @Override
    public KeyMapping keyMapping(String modId, String name, int defaultKey) {
        KeyMapping mapping = new KeyMapping("key." + modId + "." + name, defaultKey, "key.categories." + modId);
        modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(mapping));
        return mapping;
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            throw new IllegalStateException("Modularity: cannot send a payload without a server connection");
        }
        connection.send(payload);
    }

    @Override
    public <P extends CustomPacketPayload> void onClientPayload(CustomPacketPayload.Type<P> type,
                                                                Consumer<P> handler) {
        NeoForgePayloads.onClient(type, handler);
    }

    @Override
    public void entityRenderer(Id id, EntityRendererProvider<ModularEntity> provider) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerEntityRenderer(Wrap.entityType(id), provider));
    }

    /** Replaces the baked model of every state of each block, so the model decides its quads at render time. */
    @Override
    public void blockModels(Map<Id, DynamicModel> models) {
        if (models.isEmpty()) {
            return;
        }
        modBus.addListener((ModelEvent.ModifyBakingResult event) -> {
            Map<ModelResourceLocation, BakedModel> baked = event.getModels();
            models.forEach((id, model) -> {
                BakedModel dynamic = new NeoForgeDynamicBakedModel(model);
                for (BlockState state : Wrap.block(id).getStateDefinition().getPossibleStates()) {
                    baked.put(BlockModelShaper.stateToModelLocation(state), dynamic);
                }
            });
        });
    }

    /** Draws the portable HUD layers on top of the vanilla HUD, unless the GUI is hidden or a screen is open. */
    @Override
    public void hud(List<HudLayer> layers) {
        NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.options.hideGui || minecraft.screen != null || minecraft.player == null) {
                return;
            }
            var context = new McHudContext(event.getGuiGraphics(), minecraft);
            layers.forEach(layer -> layer.render(context));
        });
    }

    @Override
    public void onResourceReload(Runnable action) {
        modBus.addListener((RegisterClientReloadListenersEvent event) ->
                event.registerReloadListener(new RunnableReloadListener(action)));
    }

    @Override
    public void onClientTick(Consumer<Minecraft> action) {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> action.accept(Minecraft.getInstance()));
    }
}
