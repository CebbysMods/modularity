package lv.cebbys.mcmods.modularity.platform.loader;

import lv.cebbys.mcmods.modularity.api.client.DynamicModel;
import lv.cebbys.mcmods.modularity.api.client.HudLayer;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.platform.entity.ModularEntity;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** The client part of {@link LoaderHooks}; installed only on the client, so servers never load client classes. */
public interface ClientHooks {

    /** A key mapping {@code key.<modId>.<name>} in the mod's own category. */
    KeyMapping keyMapping(String modId, String name, int defaultKey);

    void sendToServer(CustomPacketPayload payload);

    /** Handles {@code type} arriving on the client, on the client thread. */
    <P extends CustomPacketPayload> void onClientPayload(CustomPacketPayload.Type<P> type, Consumer<P> handler);

    /**
     * Draws the registered entity of {@code id} with {@code provider}. The loader resolves the entity type when its
     * renderer registry is open, because the Forge family runs the client stage before its registries are readable
     * (docs/07 7.8).
     */
    void entityRenderer(Id id, EntityRendererProvider<ModularEntity> provider);

    /**
     * Every state of each block renders through its dynamic model (the map fills while mods initialize). The loader
     * resolves the blocks when it bakes models, for the same reason as {@link #entityRenderer}.
     */
    void blockModels(Map<Id, DynamicModel> models);

    /** Draws {@code layers} over the vanilla HUD, unless the GUI is hidden or a screen is open. */
    void hud(List<HudLayer> layers);

    /** Runs {@code action} after client resources reload. */
    void onResourceReload(Runnable action);

    /** Runs {@code action} at the end of every client tick. */
    void onClientTick(Consumer<Minecraft> action);
}
