package lv.cebbys.mcmods.modularity.platform.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import lv.cebbys.mcmods.modularity.api.client.ModelRef;
import lv.cebbys.mcmods.modularity.api.client.RenderOutput;
import lv.cebbys.mcmods.modularity.api.client.model.ObjModel;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.world.Rotation;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.util.List;

/**
 * {@link RenderOutput} on a pose stack: transforms change the stack, models become entity quads. How the quads reach
 * the renderer ({@link #draw}) depends on the version's render pipeline.
 */
abstract class PoseOutput implements RenderOutput {
    protected final PoseStack pose;
    protected final int light;

    PoseOutput(PoseStack pose, int light) {
        this.pose = pose;
        this.light = light;
    }

    /** Draws {@code faces} with {@code texture} at the current pose. */
    protected abstract void draw(Id texture, List<ObjModel.Face> faces);

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
    public void rotate(Rotation r) {
        pose.mulPose(new org.joml.Quaternionf((float) r.x(), (float) r.y(), (float) r.z(), (float) r.w()));
    }

    @Override
    public void scale(float x, float y, float z) {
        pose.scale(x, y, z);
    }

    @Override
    public void model(ModelRef model, Id texture) {
        List<ObjModel.Face> faces = new java.util.ArrayList<>();
        for (String group : model.model().groupNames()) {
            faces.addAll(model.model().faces(group));
        }
        draw(texture, faces);
    }

    @Override
    public void model(ModelRef model, String group, Id texture) {
        draw(texture, model.model().faces(group));
    }

    /** Writes the faces as entity quads; triangles repeat their last vertex. */
    static void emit(VertexConsumer consumer, PoseStack.Pose last, List<ObjModel.Face> faces, int light) {
        for (ObjModel.Face face : faces) {
            List<ObjModel.Vertex> vertices = face.vertices();
            for (int i = 0; i < 4; i++) {
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
