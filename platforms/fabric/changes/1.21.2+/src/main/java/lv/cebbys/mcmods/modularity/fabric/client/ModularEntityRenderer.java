package lv.cebbys.mcmods.modularity.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import lv.cebbys.mcmods.modularity.api.client.EntityRenderer;
import lv.cebbys.mcmods.modularity.fabric.entity.ModularEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Draws a Modularity entity with a portable {@link EntityRenderer} (see {@link EntityDrawing}). 1.21.2: renderers
 * draw from a render state extracted each frame.
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
    public void render(State state, PoseStack poseStack, MultiBufferSource buffers, int light) {
        EntityDrawing.draw(renderer, state.entity, state.partialTick, poseStack, buffers, light);
        super.render(state, poseStack, buffers, light);
    }
}
