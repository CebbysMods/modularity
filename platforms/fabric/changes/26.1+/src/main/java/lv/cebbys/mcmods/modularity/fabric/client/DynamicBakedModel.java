package lv.cebbys.mcmods.modularity.fabric.client;

import lv.cebbys.mcmods.modularity.api.client.DynamicModel;
import lv.cebbys.mcmods.modularity.fabric.Wrap;
import lv.cebbys.mcmods.modularity.fabric.world.NativeBlockView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Block state model that asks a portable {@link DynamicModel} what to render and delegates to the models of the
 * emitted block states through the Fabric Renderer API (Indigo, Sodium, Iris). 26.1: models come from the model
 * manager's block state model set; material flags (translucency etc.) are those of the emitted delegates.
 */
final class DynamicBakedModel implements BlockStateModel {
    private final DynamicModel model;
    private final Material.Baked particle;

    DynamicBakedModel(DynamicModel model, Material.Baked particle) {
        this.model = model;
        this.particle = particle;
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter view, BlockPos pos, BlockState state,
                          RandomSource random, Predicate<Direction> cullTest) {
        forEachDelegate(view, pos, state, (delegate, nativeState) ->
                delegate.emitQuads(emitter, view, pos, nativeState, random, cullTest));
    }

    @Override
    public int materialFlags(BlockAndTintGetter view, BlockPos pos, BlockState state, RandomSource random) {
        int[] flags = {0};
        forEachDelegate(view, pos, state, (delegate, nativeState) ->
                flags[0] |= delegate.materialFlags(view, pos, nativeState, random));
        return flags[0];
    }

    private void forEachDelegate(BlockAndTintGetter view, BlockPos pos, BlockState state,
                                 BiConsumer<BlockStateModel, BlockState> action) {
        var models = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        model.emit(new NativeBlockView(view), Wrap.pos(pos), Wrap.state(state), emitted -> {
            BlockState nativeState = Wrap.state(emitted);
            BlockStateModel delegate = models.get(nativeState);
            if (delegate != this) {
                action.accept(delegate, nativeState);
            }
        });
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
        // No vanilla parts: everything is emitted through the Fabric Renderer API.
    }

    @Override
    public Material.Baked particleMaterial() {
        return particle;
    }

    @Override
    public int materialFlags() {
        // Without a position the emitted states are unknown; positioned rendering uses the delegates' flags.
        return 0;
    }
}
