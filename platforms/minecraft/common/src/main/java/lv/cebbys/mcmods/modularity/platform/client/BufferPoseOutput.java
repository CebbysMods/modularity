package lv.cebbys.mcmods.modularity.platform.client;

import com.mojang.blaze3d.vertex.PoseStack;
import lv.cebbys.mcmods.modularity.api.client.model.ObjModel;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.platform.compat.Ids;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.util.List;

/** {@link PoseOutput} writing straight into the frame's buffer source. */
final class BufferPoseOutput extends PoseOutput {
    private final MultiBufferSource buffers;

    BufferPoseOutput(PoseStack pose, MultiBufferSource buffers, int light) {
        super(pose, light);
        this.buffers = buffers;
    }

    @Override
    protected void draw(Id texture, List<ObjModel.Face> faces) {
        emit(buffers.getBuffer(RenderType.entityCutoutNoCull(Ids.rl(texture))), pose.last(), faces, light);
    }
}
