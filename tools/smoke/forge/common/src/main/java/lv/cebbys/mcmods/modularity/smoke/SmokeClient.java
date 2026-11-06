package lv.cebbys.mcmods.modularity.smoke;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/**
 * Client smoke test, Forge: the client opens the world written by the dedicated server run
 * ({@code --quickPlaySingleplayer}), renders for a few seconds, takes a screenshot and quits. Only loaded on the
 * client ({@link SmokeMod} checks the side).
 */
final class SmokeClient {
    private static final int SHOT_TICK = 100;
    private static final int STOP_TICK = 140;

    private static int ticks;

    private SmokeClient() {
    }

    static void install() {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent.Post event) -> tick(Minecraft.getInstance()));
    }

    private static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return;
        }
        ticks++;
        if (ticks == 1) {
            SmokeMod.LOGGER.info("[smoke] client joined world");
        }
        if (ticks == SHOT_TICK) {
            Screenshots.take(mc, "smoke.png");
        }
        if (ticks == STOP_TICK) {
            SmokeMod.LOGGER.info("[smoke] client done");
            mc.stop();
        }
    }
}
