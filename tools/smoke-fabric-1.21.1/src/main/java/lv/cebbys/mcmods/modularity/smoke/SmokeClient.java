package lv.cebbys.mcmods.modularity.smoke;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.level.levelgen.Heightmap;

/** In the client: frames the slabs, waits for chunks to mesh, takes screenshots and quits. */
public final class SmokeClient implements ClientModInitializer {
    private int ticks;
    private int y;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return;
        }
        ticks++;
        if (ticks == 40) {
            y = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, -2);
            mc.options.hideGui = true;
        }
        if (ticks == 40 || ticks == 260) {
            mc.player.moveTo(0.5D, y, -2.5D, 0.0F, 28.0F);
        }
        if (ticks == 240) {
            shot(mc, "refabslab-overview.png");
        }
        if (ticks == 260) {
            mc.player.moveTo(-1.5D, y, 1.2D, -20.0F, 38.0F);
        }
        if (ticks == 420) {
            shot(mc, "refabslab-closeup.png");
        }
        if (ticks == 480) {
            SmokeServer.LOGGER.info("[smoke] client done");
            mc.stop();
        }
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(),
                message -> SmokeServer.LOGGER.info("[smoke] {}", message.getString()));
    }
}
