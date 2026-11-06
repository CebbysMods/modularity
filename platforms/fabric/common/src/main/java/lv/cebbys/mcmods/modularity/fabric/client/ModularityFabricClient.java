package lv.cebbys.mcmods.modularity.fabric.client;

import lv.cebbys.mcmods.modularity.api.Modularity;
import lv.cebbys.mcmods.modularity.api.Platform;
import lv.cebbys.mcmods.modularity.api.block.BlockHandle;
import lv.cebbys.mcmods.modularity.api.client.CameraController;
import lv.cebbys.mcmods.modularity.api.client.ClientContext;
import lv.cebbys.mcmods.modularity.api.client.ClientState;
import lv.cebbys.mcmods.modularity.api.client.DynamicModel;
import lv.cebbys.mcmods.modularity.api.client.EntityRenderer;
import lv.cebbys.mcmods.modularity.api.client.HitParticles;
import lv.cebbys.mcmods.modularity.api.client.HudLayer;
import lv.cebbys.mcmods.modularity.api.client.KeyBinding;
import lv.cebbys.mcmods.modularity.api.client.LoopingSound;
import lv.cebbys.mcmods.modularity.api.client.ModelRef;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.entity.EntityHandle;
import lv.cebbys.mcmods.modularity.api.network.Channel;
import lv.cebbys.mcmods.modularity.fabric.ModularityFabric;
import lv.cebbys.mcmods.modularity.fabric.Wrap;
import lv.cebbys.mcmods.modularity.fabric.entity.ModularEntity;
import lv.cebbys.mcmods.modularity.fabric.network.FabricChannel;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Fabric client entrypoint: runs {@code initClient} of every Modularity mod and wires client features. */
public final class ModularityFabricClient implements ClientModInitializer {
    private static final List<Consumer<ClientState>> TICK_HANDLERS = new ArrayList<>();
    private static final List<HudLayer> HUD_LAYERS = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        FabricChannel.ClientSender.install(ClientPlayNetworking::send);
        for (var loaded : ModularityFabric.mods()) {
            try {
                loaded.mod().initClient(new Context(loaded.modId()));
            } catch (RuntimeException e) {
                throw new IllegalStateException("Modularity mod '" + loaded.modId() + "' failed client initialization", e);
            }
        }
        ModelLoadingPlugin.register(plugin -> ClientRegistry.MODELS.forEach((block, model) ->
                plugin.registerBlockStateResolver(block, context -> {
                    var unbaked = new DynamicUnbakedModel(model);
                    for (var state : block.getStateDefinition().getPossibleStates()) {
                        context.setModel(state, unbaked);
                    }
                })));
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            if (minecraft.level == null) {
                return;
            }
            ClientState state = FabricClientState.INSTANCE;
            TICK_HANDLERS.forEach(handler -> handler.accept(state));
            CameraState.update(minecraft);
        });
        HudRenderCallback.EVENT.register((graphics, delta) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.options.hideGui || minecraft.screen != null || minecraft.player == null) {
                return;
            }
            var ctx = new FabricHudContext(graphics, minecraft);
            HUD_LAYERS.forEach(layer -> layer.render(ctx));
        });
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return ResourceLocation.fromNamespaceAndPath("modularity", "obj_models");
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                FabricModelRef.clearAll();
            }
        });
    }

    private record Context(String modId) implements ClientContext {
        @Override
        public Platform platform() {
            return Modularity.platform();
        }

        @Override
        public void blockModel(BlockHandle block, DynamicModel model) {
            ClientRegistry.model(Wrap.block(block.get()), model);
        }

        @Override
        public void hitParticles(BlockHandle block, HitParticles particles) {
            ClientRegistry.hitParticles(Wrap.block(block.get()), particles);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void entityRenderer(EntityHandle type, EntityRenderer renderer) {
            var nativeType = (EntityType<ModularEntity>) BuiltInRegistries.ENTITY_TYPE.getOptional(Wrap.rl(type.id())).orElseThrow();
            EntityRendererRegistry.register(nativeType, ctx -> new ModularEntityRenderer(ctx, renderer));
        }

        @Override
        public ModelRef objModel(Id resource) {
            return FabricModelRef.of(resource);
        }

        @Override
        public KeyBinding keyBinding(String name, int defaultKey) {
            KeyMapping mapping = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                    "key." + modId + "." + name, defaultKey, "key.categories." + modId));
            return new KeyBinding() {
                @Override
                public String name() {
                    return name;
                }

                @Override
                public boolean isDown() {
                    return mapping.isDown();
                }

                @Override
                public boolean consumeClick() {
                    return mapping.consumeClick();
                }
            };
        }

        @Override
        public void onTick(Consumer<ClientState> handler) {
            TICK_HANDLERS.add(handler);
        }

        @Override
        public <T> void onMessage(Channel<T> channel, Consumer<T> handler) {
            FabricChannel<T> fabricChannel = (FabricChannel<T>) channel;
            ClientPlayNetworking.registerGlobalReceiver(fabricChannel.type(),
                    (payload, context) -> handler.accept(fabricChannel.decode(payload)));
        }

        @Override
        public void camera(CameraController controller) {
            CameraState.register(controller);
        }

        @Override
        public void hud(HudLayer layer) {
            HUD_LAYERS.add(layer);
        }

        @Override
        public void play(LoopingSound sound) {
            Minecraft.getInstance().getSoundManager().play(new FabricLoopingSound(sound));
        }

        @Override
        public ClientState state() {
            return FabricClientState.INSTANCE;
        }
    }
}
