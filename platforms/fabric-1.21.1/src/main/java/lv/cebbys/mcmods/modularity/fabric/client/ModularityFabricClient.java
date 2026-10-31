package lv.cebbys.mcmods.modularity.fabric.client;

import lv.cebbys.mcmods.modularity.api.Platform;
import lv.cebbys.mcmods.modularity.api.block.BlockHandle;
import lv.cebbys.mcmods.modularity.api.client.ClientContext;
import lv.cebbys.mcmods.modularity.api.client.DynamicModel;
import lv.cebbys.mcmods.modularity.api.client.HitParticles;
import lv.cebbys.mcmods.modularity.api.Modularity;
import lv.cebbys.mcmods.modularity.fabric.ModularityFabric;
import lv.cebbys.mcmods.modularity.fabric.Wrap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;

/** Fabric client entrypoint: runs {@code initClient} of every Modularity mod and hooks models in. */
public final class ModularityFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
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
    }
}
