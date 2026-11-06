package lv.cebbys.mcmods.modularity.platform.client;

import com.mojang.blaze3d.vertex.PoseStack;
import lv.cebbys.mcmods.modularity.api.client.EntityRenderer;
import lv.cebbys.mcmods.modularity.platform.entity.ModularEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Draws a Modularity entity with a portable {@link EntityRenderer} (see {@link EntityDrawing}). */
final class ModularEntityRenderer extends net.minecraft.client.renderer.entity.EntityRenderer<ModularEntity> {
    private static final ResourceLocation NONE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private final EntityRenderer renderer;

    ModularEntityRenderer(EntityRendererProvider.Context context, EntityRenderer renderer) {
        super(context);
        this.renderer = renderer;
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(ModularEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        EntityDrawing.draw(renderer, entity, partialTick, poseStack, new BufferPoseOutput(poseStack, buffers, light));
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ModularEntity entity) {
        return NONE;
    }
}
