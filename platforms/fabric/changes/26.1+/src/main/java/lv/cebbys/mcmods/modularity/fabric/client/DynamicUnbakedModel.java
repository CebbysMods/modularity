package lv.cebbys.mcmods.modularity.fabric.client;

import lv.cebbys.mcmods.modularity.api.client.DynamicModel;
import lv.cebbys.mcmods.modularity.platform.compat.Ids;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.world.level.block.state.BlockState;

/** 26.1: block state models live in {@code block.dispatch}; sprites are baked {@link Material}s. */
final class DynamicUnbakedModel implements BlockStateModel.UnbakedRoot {
    private final DynamicModel model;

    DynamicUnbakedModel(DynamicModel model) {
        this.model = model;
    }

    @Override
    public void resolveDependencies(Resolver resolver) {
        // No parent or child models: everything is emitted at render time.
    }

    @Override
    public BlockStateModel bake(BlockState state, ModelBaker baker) {
        var particle = baker.materials().get(new Material(Ids.rl(model.particleTexture())),
                () -> "modularity dynamic model " + model.particleTexture());
        return new DynamicBakedModel(model, particle);
    }

    @Override
    public Object visualEqualityGroup(BlockState state) {
        // One model for every state of the block.
        return this;
    }
}
