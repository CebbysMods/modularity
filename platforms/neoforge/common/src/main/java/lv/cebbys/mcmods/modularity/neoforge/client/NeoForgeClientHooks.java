package lv.cebbys.mcmods.modularity.neoforge.client;

import lv.cebbys.mcmods.modularity.api.client.DynamicModel;
import lv.cebbys.mcmods.modularity.api.client.HudLayer;
import lv.cebbys.mcmods.modularity.platform.client.RunnableReloadListener;
import lv.cebbys.mcmods.modularity.platform.entity.ModularEntity;
import lv.cebbys.mcmods.modularity.platform.loader.ClientHooks;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The NeoForge client seam. The tick handler uses the game bus; the reload listener uses the mod bus, because
 * {@link RegisterClientReloadListenersEvent} is a mod-bus event.
 */
public final class NeoForgeClientHooks implements ClientHooks {
    private static final Logger LOGGER = LoggerFactory.getLogger("modularity");

    private final IEventBus modBus;
    private boolean warnedModels;
    private boolean warnedHud;

    public NeoForgeClientHooks(IEventBus modBus) {
        this.modBus = modBus;
    }

    @Override
    public KeyMapping keyMapping(String modId, String name, int defaultKey) {
        throw notImplemented("keyMapping");
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        throw notImplemented("sendToServer");
    }

    @Override
    public <P extends CustomPacketPayload> void onClientPayload(CustomPacketPayload.Type<P> type,
                                                                Consumer<P> handler) {
        throw notImplemented("onClientPayload");
    }

    @Override
    public void entityRenderer(EntityType<ModularEntity> type, EntityRendererProvider<ModularEntity> provider) {
        throw notImplemented("entityRenderer");
    }

    /** Dynamic block models need a loader model hook, which is not wired on NeoForge yet. */
    @Override
    public void blockModels(Map<Block, DynamicModel> models) {
        if (!models.isEmpty() && !warnedModels) {
            warnedModels = true;
            LOGGER.warn("Modularity dynamic block models are not implemented on NeoForge yet; {} model(s) ignored",
                    models.size());
        }
    }

    /** The HUD hook is not wired on NeoForge yet. */
    @Override
    public void hud(List<HudLayer> layers) {
        if (!layers.isEmpty() && !warnedHud) {
            warnedHud = true;
            LOGGER.warn("Modularity HUD layers are not implemented on NeoForge yet; {} layer(s) ignored",
                    layers.size());
        }
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

    private static UnsupportedOperationException notImplemented(String method) {
        return new UnsupportedOperationException(
                "Modularity client feature is not implemented on NeoForge yet (" + method + ")");
    }
}
