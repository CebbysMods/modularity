package lv.cebbys.mcmods.modularity.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import lv.cebbys.mcmods.modularity.api.client.ModelRef;
import lv.cebbys.mcmods.modularity.api.client.RenderOutput;
import lv.cebbys.mcmods.modularity.api.client.model.ObjModel;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.fabric.Wrap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.util.List;

/** {@link RenderOutput} on a pose stack and buffer source: OBJ faces as entity quads. */
record PoseOutput(PoseStack pose, MultiBufferSource buffers, int light) implements RenderOutput {
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
    public void rotate(lv.cebbys.mcmods.modularity.api.world.Rotation r) {
        pose.mulPose(new org.joml.Quaternionf((float) r.x(), (float) r.y(), (float) r.z(), (float) r.w()));
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