package lv.cebbys.mcmods.modularity.smoke;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

/** Screenshot of the current frame into {@code screenshots/}; the signature changes between versions. */
final class Screenshots {
    private Screenshots() {
    }

    static void take(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(),
                message -> SmokeMod.LOGGER.info("[smoke] screenshot: {}", message.getString()));
    }
}
