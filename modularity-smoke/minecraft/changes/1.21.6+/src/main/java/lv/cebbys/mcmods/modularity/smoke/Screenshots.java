package lv.cebbys.mcmods.modularity.smoke;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

/** Screenshot of the current frame into {@code screenshots/}. 1.21.6: takes a downscale factor (1 = full size). */
final class Screenshots {
    private Screenshots() {
    }

    static void take(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1,
                message -> SmokeMod.LOGGER.info("[smoke] screenshot: {}", message.getString()));
    }
}
