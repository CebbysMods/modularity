package lv.cebbys.mcmods.modularity.forge.client;

import lv.cebbys.mcmods.modularity.api.client.DynamicModel;
import lv.cebbys.mcmods.modularity.api.client.HudLayer;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.forge.network.ForgePayloads;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The Forge client seam. The tick and the payload handlers use the game bus; the key mappings, the entity renderers,
 * the reload listener, the HUD layers and the dynamic block models use the mod bus, because their events are mod-bus
 * events.
 */
public final class ForgeClientHooks implements ClientHooks {
    private final IEventBus modBus;
    private int hudLayers;

    public ForgeClientHooks(IEventBus modBus) {
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
        ForgePayloads.sendToServer(payload);
    }

    @Override
    public <P extends CustomPacketPayload> void onClientPayload(CustomPacketPayload.Type<P> type,
                                                                Consumer<P> handler) {
        ForgePayloads.onClient(type, handler);
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
                BakedModel dynamic = new ForgeDynamicBakedModel(model);
                for (BlockState state : Wrap.block(id).getStateDefinition().getPossibleStates()) {
                    baked.put(BlockModelShaper.stateToModelLocation(state), dynamic);
                }
            });
        });
    }

    /**
     * Draws the portable HUD layers above the vanilla HUD, unless the GUI is hidden or a screen is open. The layer is
     * appended to the root stack: the stack of Forge 52 has no hotbar layer to anchor on, and Forge logs a warning and
     * drops the layer when the anchor is absent.
     */
    @Override
    public void hud(List<HudLayer> layers) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("modularity", "hud_" + (++hudLayers));
        modBus.addListener((AddGuiOverlayLayersEvent event) -> event.getLayeredDraw().add(id,
                (graphics, delta) -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    if (minecraft.options.hideGui || minecraft.screen != null || minecraft.player == null) {
                        return;
                    }
                    McHudContext context = new McHudContext(graphics, minecraft);
                    layers.forEach(layer -> layer.render(context));
                }));
    }

    @Override
    public void onResourceReload(Runnable action) {
        modBus.addListener((RegisterClientReloadListenersEvent event) ->
                event.registerReloadListener(new RunnableReloadListener(action)));
    }

    @Override
    public void onClientTick(Consumer<Minecraft> action) {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent.Post event) ->
                action.accept(Minecraft.getInstance()));
    }
}
