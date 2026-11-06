package lv.cebbys.mcmods.modularity.fabric.client;

import lv.cebbys.mcmods.modularity.api.client.DynamicModel;
import lv.cebbys.mcmods.modularity.fabric.Wrap;
import lv.cebbys.mcmods.modularity.fabric.world.NativeBlockView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Baked model that asks a portable {@link DynamicModel} what to render and delegates to the models
 * of the emitted block states through the Fabric Renderer API (Indigo, Sodium, Iris). 1.21.4: quads go to a
 * {@link QuadEmitter} with a cull test instead of a render context.
 */
final class DynamicBakedModel implements BakedModel, FabricBakedModel {
    private final DynamicModel model;
    private final TextureAtlasSprite particle;

    DynamicBakedModel(DynamicModel model, TextureAtlasSprite particle) {
        this.model = model;
        this.particle = particle;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(QuadEmitter emitter, BlockAndTintGetter view, BlockState state, BlockPos pos,
                               Supplier<RandomSource> random, Predicate<Direction> cullTest) {
        var shaper = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper();
        model.emit(new NativeBlockView(view), Wrap.pos(pos), Wrap.state(state), emitted -> {
            BlockState nativeState = Wrap.state(emitted);
            BakedModel delegate = shaper.getBlockModel(nativeState);
            if (delegate != this) {
                ((FabricBakedModel) delegate).emitBlockQuads(emitter, view, nativeState, pos, random, cullTest);
            }
        });
    }

    @Override
    public void emitItemQuads(QuadEmitter emitter, Supplier<RandomSource> random) {
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction direction, RandomSource random) {
        return List.of();
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
    public TextureAtlasSprite getParticleIcon() {
        return particle;
    }

    @Override
    public ItemTransforms getTransforms() {
        return ItemTransforms.NO_TRANSFORMS;
    }
}
