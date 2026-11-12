package lv.cebbys.mcmods.modularity.loom.client;

import lv.cebbys.mcmods.modularity.api.client.DynamicModel;
import lv.cebbys.mcmods.modularity.platform.Wrap;
import lv.cebbys.mcmods.modularity.platform.world.NativeBlockView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Predicate;

/**
 * Block state model that asks a portable {@link DynamicModel} what to render and delegates to the models of the
 * emitted block states through the Fabric Renderer API (Indigo, Sodium, Iris). 1.21.5: vanilla block models are
 * {@link BlockStateModel}s made of parts; Fabric's {@code emitQuads} replaces the baked model hooks.
 */
final class DynamicBakedModel implements BlockStateModel {
    private final DynamicModel model;
    private final TextureAtlasSprite particle;

    DynamicBakedModel(DynamicModel model, TextureAtlasSprite particle) {
        this.model = model;
        this.particle = particle;
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter view, BlockPos pos, BlockState state,
                          RandomSource random, Predicate<Direction> cullTest) {
        var shaper = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper();
        model.emit(new NativeBlockView(view), Wrap.pos(pos), Wrap.state(state), emitted -> {
            BlockState nativeState = Wrap.state(emitted);
            BlockStateModel delegate = shaper.getBlockModel(nativeState);
            if (delegate != this) {
                delegate.emitQuads(emitter, view, pos, nativeState, random, cullTest);
            }
        });
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts) {
        // No vanilla parts: everything is emitted through the Fabric Renderer API.
    }

    @Override
    public TextureAtlasSprite particleIcon() {
        return particle;
    }
}
