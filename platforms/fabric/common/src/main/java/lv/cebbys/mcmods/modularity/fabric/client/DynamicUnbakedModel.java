package lv.cebbys.mcmods.modularity.fabric.client;

import lv.cebbys.mcmods.modularity.api.client.DynamicModel;
import lv.cebbys.mcmods.modularity.fabric.compat.Ids;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

final class DynamicUnbakedModel implements UnbakedModel {
    private final DynamicModel model;

    DynamicUnbakedModel(DynamicModel model) {
        this.model = model;
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return List.of();
    }

    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> resolver) {
    }

    @Override
    public BakedModel bake(ModelBaker baker, Function<Material, TextureAtlasSprite> sprites, ModelState state) {
        var particle = sprites.apply(new Material(TextureAtlas.LOCATION_BLOCKS, Ids.rl(model.particleTexture())));
        return new DynamicBakedModel(model, particle);
    }
}
