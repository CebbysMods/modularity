package lv.cebbys.mcmods.modularity.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import lv.cebbys.mcmods.modularity.api.client.EntityRenderer;
import lv.cebbys.mcmods.modularity.api.client.ModelRef;
import lv.cebbys.mcmods.modularity.api.client.RenderOutput;
import lv.cebbys.mcmods.modularity.api.client.model.ObjModel;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.fabric.Wrap;
import lv.cebbys.mcmods.modularity.fabric.entity.ModularEntity;
import lv.cebbys.mcmods.modularity.fabric.world.NativeEntityRef;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.List;

/** Draws a Modularity entity with a portable {@link EntityRenderer}. */
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
        float lerpedYaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        var view = new NativeEntityRef(entity) {
            @Override
            public float yaw() {
                return lerpedYaw;
            }
        };
        poseStack.pushPose();
        renderer.render(view, partialTick, new Output(poseStack, buffers, light));
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ModularEntity entity) {
        return NONE;
    }

    private record Output(PoseStack pose, MultiBufferSource buffers, int light) implements RenderOutput {
        @Override
        public void push() {
            pose.pushPose();
        }

        @Override
        public void pop() {
            pose.popPose();
        }

        @Override
        public void translate(double x, double y, double z) {
            pose.translate(x, y, z);
        }

        @Override
        public void rotateX(float degrees) {
            pose.mulPose(Axis.XP.rotationDegrees(degrees));
        }

        @Override
        public void rotateY(float degrees) {
            pose.mulPose(Axis.YP.rotationDegrees(degrees));
        }

        @Override
        public void rotateZ(float degrees) {
            pose.mulPose(Axis.ZP.rotationDegrees(degrees));
        }

        @Override
        public void scale(float x, float y, float z) {
            pose.scale(x, y, z);
        }

        @Override
        public void model(ModelRef model, Id texture) {
            VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(Wrap.rl(texture)));
            for (String group : model.model().groupNames()) {
                draw(consumer, model.model().faces(group));
            }
        }

        @Override
        public void model(ModelRef model, String group, Id texture) {
            VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(Wrap.rl(texture)));
            draw(consumer, model.model().faces(group));
        }

        private void draw(VertexConsumer consumer, List<ObjModel.Face> faces) {
            PoseStack.Pose last = pose.last();
            for (ObjModel.Face face : faces) {
                List<ObjModel.Vertex> vertices = face.vertices();
                for (int i = 0; i < 4; i++) {
                    // Entity render types draw quads; triangles repeat their last vertex.
                    ObjModel.Vertex v = vertices.get(Math.min(i, vertices.size() - 1));
                    consumer.addVertex(last, v.x(), v.y(), v.z())
                            .setColor(-1)
                            .setUv(v.u(), 1.0F - v.v())
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(light)
                            .setNormal(last, v.nx(), v.ny(), v.nz());
                }
            }
        }
    }
}
