package lv.cebbys.mcmods.modularity.api.client;

import lv.cebbys.mcmods.modularity.api.world.Player;

import java.util.Optional;

/** Decides camera settings for the local player each frame; empty keeps vanilla behaviour. */
@FunctionalInterface
public interface CameraController {
    Optional<CameraSettings> camera(Player player);

    /**
     * @param thirdPerson switch to third person (back) while active; the previous view is restored afterwards
     * @param distance    third person camera distance in blocks (vanilla: 4)
     */
    record CameraSettings(boolean thirdPerson, float distance) {
    }
}
