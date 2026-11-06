package lv.cebbys.mcmods.modularity.platform.mixin.client;

import lv.cebbys.mcmods.modularity.platform.client.CameraState;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Applies the third person distance of {@code CameraController}s. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"), index = 0)
    private float modularity$cameraDistance(float distance) {
        return distance * CameraState.distanceFactor();
    }
}
