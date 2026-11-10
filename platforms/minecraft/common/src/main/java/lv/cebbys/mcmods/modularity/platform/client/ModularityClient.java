package lv.cebbys.mcmods.modularity.platform.client;

import lv.cebbys.mcmods.modularity.api.Modularity;
import lv.cebbys.mcmods.modularity.api.ModularityModClient;
import lv.cebbys.mcmods.modularity.api.ModularityModCommon;
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
import lv.cebbys.mcmods.modularity.platform.ModularityCommon;
import lv.cebbys.mcmods.modularity.platform.Wrap;
import lv.cebbys.mcmods.modularity.platform.compat.Ids;
import lv.cebbys.mcmods.modularity.platform.entity.ModularEntity;
import lv.cebbys.mcmods.modularity.platform.loader.ClientHooks;
import lv.cebbys.mcmods.modularity.platform.loader.Loaders;
import lv.cebbys.mcmods.modularity.platform.network.McChannel;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Client init, the same on every loader: runs the client stage of every Modularity mod and wires client features
 * through {@link Loaders#client()}, which the loader's client entrypoint installs first.
 */
public final class ModularityClient {
    private static final List<Consumer<ClientState>> TICK_HANDLERS = new ArrayList<>();
    private static final List<HudLayer> HUD_LAYERS = new ArrayList<>();

    private ModularityClient() {
    }

    public static void init() {
        ClientHooks client = Loaders.client();
        ModularityCommon.forEachMod("client initialization", loaded -> run(loaded.mod(), new Context(loaded.modId())));
        client.blockModels(ClientRegistry.MODELS);
        client.onClientTick(minecraft -> {
            if (minecraft.level == null) {
                return;
            }
            ClientState state = McClientState.INSTANCE;
            TICK_HANDLERS.forEach(handler -> handler.accept(state));
            CameraState.update(minecraft);
        });
        client.hud(HUD_LAYERS);
        client.onResourceReload(McModelRef::clearAll);
    }

    /** Runs the client stage of a mod that has one; a common-only mod is skipped. */
    private static void run(ModularityModCommon mod, ClientContext context) {
        if (mod instanceof ModularityModClient client) {
            client.initialize(context);
        }
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
            var nativeType = (EntityType<ModularEntity>) BuiltInRegistries.ENTITY_TYPE.getOptional(Ids.rl(type.id())).orElseThrow();
            Loaders.client().entityRenderer(nativeType, ctx -> new ModularEntityRenderer(ctx, renderer));
        }

        @Override
        public ModelRef objModel(Id resource) {
            return McModelRef.of(resource);
        }

        @Override
        public KeyBinding keyBinding(String name, int defaultKey) {
            return new Key(name, Loaders.client().keyMapping(modId, name, defaultKey));
        }

        @Override
        public void onTick(Consumer<ClientState> handler) {
            TICK_HANDLERS.add(handler);
        }

        @Override
        public <T> void onMessage(Channel<T> channel, Consumer<T> handler) {
            McChannel<T> mcChannel = (McChannel<T>) channel;
            Loaders.client().onClientPayload(mcChannel.type(), payload -> handler.accept(mcChannel.decode(payload)));
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
            Minecraft.getInstance().getSoundManager().play(new McLoopingSound(sound));
        }

        @Override
        public ClientState state() {
            return McClientState.INSTANCE;
        }
    }

    private record Key(String name, KeyMapping mapping) implements KeyBinding {
        @Override
        public boolean isDown() {
            return mapping.isDown();
        }

        @Override
        public boolean consumeClick() {
            return mapping.consumeClick();
        }
    }
}
