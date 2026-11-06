package lv.cebbys.mcmods.modularity.fabric.client;

import lv.cebbys.mcmods.modularity.api.client.DynamicModel;
import lv.cebbys.mcmods.modularity.platform.compat.Ids;
import net.minecraft.client.renderer.block.model.UnbakedBlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.world.level.block.state.BlockState;

/** 1.21.4: block state models are their own unbaked type, baked with the baker's sprites. */
final class DynamicUnbakedModel implements UnbakedBlockStateModel {
    private final DynamicModel model;

    DynamicUnbakedModel(DynamicModel model) {
        this.model = model;
    }

    @Override
    public void resolveDependencies(Resolver resolver) {
        // No parent or child models: everything is emitted at render time.
    }

    @Override
    public BakedModel bake(ModelBaker baker) {
        var particle = baker.sprites().get(new Material(TextureAtlas.LOCATION_BLOCKS, Ids.rl(model.particleTexture())));
        return new DynamicBakedModel(model, particle);
    }

    @Override
    public Object visualEqualityGroup(BlockState state) {
        // One model for every state of the block.
        return this;
    }
}
