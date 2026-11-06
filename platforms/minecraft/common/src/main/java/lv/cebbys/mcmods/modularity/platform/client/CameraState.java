package lv.cebbys.mcmods.modularity.platform.client;

import lv.cebbys.mcmods.modularity.api.client.CameraController;
import lv.cebbys.mcmods.modularity.platform.Wrap;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Applies {@link CameraController}s: forced third person and camera distance. */
public final class CameraState {
    private static final List<CameraController> CONTROLLERS = new ArrayList<>();
    private static volatile float distance = 4.0F;
    private static CameraType restore;

    private CameraState() {
    }

    static void register(CameraController controller) {
        CONTROLLERS.add(controller);
    }

    /** Distance factor applied to the vanilla third person distance (vanilla = 4 blocks). */
    public static float distanceFactor() {
        return distance / 4.0F;
    }

    static void update(Minecraft minecraft) {
        Optional<CameraController.CameraSettings> settings = Optional.empty();
        if (minecraft.player != null) {
            var player = Wrap.player(minecraft.player);
            for (CameraController controller : CONTROLLERS) {
                settings = controller.camera(player);
                if (settings.isPresent()) {
                    break;
                }
            }
        }
        if (settings.isPresent()) {
            distance = settings.get().distance();
            if (settings.get().thirdPerson() && restore == null) {
                restore = minecraft.options.getCameraType();
                minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
        } else {
            distance = 4.0F;
            if (restore != null) {
                minecraft.options.setCameraType(restore);
                restore = null;
            }
        }
    }
}
