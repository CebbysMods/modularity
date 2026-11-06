package lv.cebbys.mcmods.modularity.platform.mixin.client;

import lv.cebbys.mcmods.modularity.platform.client.CameraState;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Applies the third person distance of {@code CameraController}s. 26.1: the zoom is computed in {@code alignWithEntity}. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyArg(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"), index = 0)
    private float modularity$cameraDistance(float distance) {
        return distance * CameraState.distanceFactor();
    }
}
