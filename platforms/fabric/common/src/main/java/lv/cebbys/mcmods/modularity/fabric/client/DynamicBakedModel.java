package lv.cebbys.mcmods.modularity.fabric.client;

import lv.cebbys.mcmods.modularity.api.client.DynamicModel;
import lv.cebbys.mcmods.modularity.platform.Wrap;
import lv.cebbys.mcmods.modularity.platform.world.NativeBlockView;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Supplier;

/**
 * Baked model that asks a portable {@link DynamicModel} what to render and delegates to the models
 * of the emitted block states through the Fabric Renderer API (Indigo, Sodium, Iris).
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
    public void emitBlockQuads(BlockAndTintGetter view, BlockState state, BlockPos pos,
                               Supplier<RandomSource> random, RenderContext context) {
        var shaper = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper();
        model.emit(new NativeBlockView(view), Wrap.pos(pos), Wrap.state(state), emitted -> {
            BlockState nativeState = Wrap.state(emitted);
            BakedModel delegate = shaper.getBlockModel(nativeState);
            if (delegate != this) {
                ((FabricBakedModel) delegate).emitBlockQuads(view, nativeState, pos, random, context);
            }
        });
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> random, RenderContext context) {
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
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return particle;
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
