package lv.cebbys.mcmods.modularity.fabric.client;

import lv.cebbys.mcmods.modularity.api.client.DynamicModel;
import lv.cebbys.mcmods.modularity.platform.compat.Ids;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.world.level.block.state.BlockState;

/** 1.21.5: block state models are {@link BlockStateModel.UnbakedRoot}s, baked per state. */
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
        var material = new Material(TextureAtlas.LOCATION_BLOCKS, Ids.rl(model.particleTexture()));
        return new DynamicBakedModel(model, baker.sprites().get(material, () -> "modularity dynamic model " + model.particleTexture()));
    }

    @Override
    public Object visualEqualityGroup(BlockState state) {
        // One model for every state of the block.
        return this;
    }
}
