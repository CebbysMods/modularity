package lv.cebbys.mcmods.modularity.smoke;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

/**
 * Client smoke test: the client opens the world written by the dedicated server run ({@code --quickPlaySingleplayer}),
 * renders for a few seconds, takes a screenshot and quits.
 */
public final class SmokeClient implements ClientModInitializer {
    private static final int SHOT_TICK = 100;
    private static final int STOP_TICK = 140;

    private int ticks;

    @Override
    public void onInitializeClient() {
        LoomStorageProbe.install();
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return;
        }
        ticks++;
        if (ticks == 1) {
            SmokeMod.LOGGER.info("Client joined world");
        }
        if (ticks == SHOT_TICK) {
            Screenshots.take(mc, "smoke.png");
        }
        if (ticks == STOP_TICK) {
            SmokeMod.LOGGER.info("Client done");
            mc.stop();
        }
    }
}
