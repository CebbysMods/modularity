package lv.cebbys.mcmods.modularity.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import lv.cebbys.mcmods.modularity.api.client.EntityRenderer;
import lv.cebbys.mcmods.modularity.api.world.Rotation;
import lv.cebbys.mcmods.modularity.fabric.entity.ModularEntity;
import lv.cebbys.mcmods.modularity.fabric.world.NativeEntityRef;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;

/** Draws one frame of a Modularity entity with its portable {@link EntityRenderer}; independent of the vanilla renderer API. */
final class EntityDrawing {
    private EntityDrawing() {
    }

    static void draw(EntityRenderer renderer, ModularEntity entity, float partialTick, PoseStack poseStack,
                     MultiBufferSource buffers, int light) {
        float lerpedYaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        var lerpedOrientation = entity.orientation(partialTick);
        var view = new NativeEntityRef(entity) {
            @Override
            public float yaw() {
                return lerpedYaw;
            }

            @Override
            public Rotation orientation() {
                return lerpedOrientation;
            }
        };
        poseStack.pushPose();
        renderer.render(view, partialTick, new PoseOutput(poseStack, buffers, light));
        poseStack.popPose();
    }
}
