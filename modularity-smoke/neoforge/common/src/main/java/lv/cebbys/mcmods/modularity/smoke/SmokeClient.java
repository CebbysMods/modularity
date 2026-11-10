package lv.cebbys.mcmods.modularity.smoke;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client smoke test, NeoForge: the client opens the world written by the dedicated server run
 * ({@code --quickPlaySingleplayer}), renders for a few seconds, takes a screenshot and quits.
 */
@Mod(value = "modularity_smoke", dist = Dist.CLIENT)
public final class SmokeClient {
    private static final int SHOT_TICK = 100;
    private static final int STOP_TICK = 140;

    private int ticks;

    public SmokeClient() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick(Minecraft.getInstance()));
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
