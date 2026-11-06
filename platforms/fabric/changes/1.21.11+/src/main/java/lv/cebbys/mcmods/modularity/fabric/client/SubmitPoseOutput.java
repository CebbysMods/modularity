package lv.cebbys.mcmods.modularity.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import lv.cebbys.mcmods.modularity.api.client.model.ObjModel;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.fabric.compat.Ids;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;

import java.util.List;

/**
 * {@link PoseOutput} submitting custom geometry to the frame's node collector (1.21.9 render pipeline). 1.21.11: render
 * types come from {@code RenderTypes}.
 */
final class SubmitPoseOutput extends PoseOutput {
    private final SubmitNodeCollector collector;

    SubmitPoseOutput(PoseStack pose, SubmitNodeCollector collector, int light) {
        super(pose, light);
        this.collector = collector;
    }

    @Override
    protected void draw(Id texture, List<ObjModel.Face> faces) {
        collector.submitCustomGeometry(pose, RenderTypes.entityCutoutNoCull(Ids.rl(texture)),
                (last, consumer) -> emit(consumer, last, faces, light));
    }
}
