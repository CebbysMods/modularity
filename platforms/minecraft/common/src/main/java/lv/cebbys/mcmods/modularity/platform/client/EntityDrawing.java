package lv.cebbys.mcmods.modularity.platform.client;

import com.mojang.blaze3d.vertex.PoseStack;
import lv.cebbys.mcmods.modularity.api.client.EntityRenderer;
import lv.cebbys.mcmods.modularity.api.client.RenderOutput;
import lv.cebbys.mcmods.modularity.api.world.Rotation;
import lv.cebbys.mcmods.modularity.platform.entity.ModularEntity;
import lv.cebbys.mcmods.modularity.platform.world.NativeEntityRef;
import net.minecraft.util.Mth;

/** Draws one frame of a Modularity entity with its portable {@link EntityRenderer}; independent of the vanilla renderer API. */
final class EntityDrawing {
    private EntityDrawing() {
    }

    static void draw(EntityRenderer renderer, ModularEntity entity, float partialTick, PoseStack poseStack,
                     RenderOutput output) {
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
        renderer.render(view, partialTick, output);
        poseStack.popPose();
    }
}
