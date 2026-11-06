package lv.cebbys.mcmods.modularity.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import lv.cebbys.mcmods.modularity.api.client.EntityRenderer;
import lv.cebbys.mcmods.modularity.fabric.entity.ModularEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Draws a Modularity entity with a portable {@link EntityRenderer} (see {@link EntityDrawing}). 1.21.9: renderers
 * submit geometry to a {@link SubmitNodeCollector} instead of writing to buffers. 26.1: camera state moved.
 */
final class ModularEntityRenderer
        extends net.minecraft.client.renderer.entity.EntityRenderer<ModularEntity, ModularEntityRenderer.State> {

    /** The portable renderer reads the entity itself, so the state only carries it and the frame time. */
    static final class State extends EntityRenderState {
        ModularEntity entity;
        float partialTick;
    }

    private final EntityRenderer renderer;

    ModularEntityRenderer(EntityRendererProvider.Context context, EntityRenderer renderer) {
        super(context);
        this.renderer = renderer;
        this.shadowRadius = 0.0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ModularEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.entity = entity;
        state.partialTick = partialTick;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        EntityDrawing.draw(renderer, state.entity, state.partialTick, poseStack,
                new SubmitPoseOutput(poseStack, collector, state.lightCoords));
        super.submit(state, poseStack, collector, camera);
    }
}
