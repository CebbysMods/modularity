package lv.cebbys.mcmods.modularity.neoforge.client;

import lv.cebbys.mcmods.modularity.api.client.DynamicModel;
import lv.cebbys.mcmods.modularity.platform.Wrap;
import lv.cebbys.mcmods.modularity.platform.compat.Ids;
import lv.cebbys.mcmods.modularity.platform.world.NativeBlockView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Baked model that asks a portable {@link DynamicModel} what to render and delegates to the models of the emitted
 * block states. NeoForge gives a model the world position in {@link #getModelData}, so the emitted states are
 * computed there and stored in the model data of the position; {@link #getQuads} then returns the quads of those
 * states.
 */
final class NeoForgeDynamicBakedModel implements BakedModel {
    private static final ModelProperty<List<BlockState>> EMITTED = new ModelProperty<>();

    private final DynamicModel model;

    NeoForgeDynamicBakedModel(DynamicModel model) {
        this.model = model;
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
        List<BlockState> emitted = new ArrayList<>();
        model.emit(new NativeBlockView(level), Wrap.pos(pos), Wrap.state(state),
                next -> emitted.add(Wrap.state(next)));
        return ModelData.builder().with(EMITTED, emitted).build();
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data,
                                    RenderType renderType) {
        List<BlockState> emitted = data.get(EMITTED);
        if (emitted == null || emitted.isEmpty()) {
            return List.of();
        }
        var shaper = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper();
        List<BakedQuad> quads = new ArrayList<>();
        for (BlockState next : emitted) {
            BakedModel delegate = shaper.getBlockModel(next);
            if (delegate != this) {
                quads.addAll(delegate.getQuads(next, side, random, ModelData.EMPTY, renderType));
            }
        }
        return quads;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
        return List.of();
    }

    @Override
    @SuppressWarnings("removal")
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
        List<BlockState> emitted = data.get(EMITTED);
        if (emitted == null || emitted.isEmpty()) {
            return ItemBlockRenderTypes.getRenderLayers(state);
        }
        var shaper = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper();
        Set<ChunkRenderTypeSet> sets = new LinkedHashSet<>();
        for (BlockState next : emitted) {
            BakedModel delegate = shaper.getBlockModel(next);
            if (delegate != this) {
                sets.add(delegate.getRenderTypes(next, random, ModelData.EMPTY));
            }
        }
        return sets.isEmpty() ? ItemBlockRenderTypes.getRenderLayers(state) : ChunkRenderTypeSet.union(sets);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        var atlas = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);
        return atlas.apply(Ids.rl(model.particleTexture()));
    }

    @Override
    public ItemTransforms getTransforms() {
        return ItemTransforms.NO_TRANSFORMS;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}
