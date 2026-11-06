package lv.cebbys.mcmods.modularity.fabric.client;

import lv.cebbys.mcmods.modularity.api.client.HudLayer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;

import java.util.List;

/** Draws the portable HUD layers on top of the vanilla HUD, unless the GUI is hidden or a screen is open. */
final class HudHook {
    private HudHook() {
    }

    static void install(List<HudLayer> layers) {
        HudRenderCallback.EVENT.register((graphics, delta) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.options.hideGui || minecraft.screen != null || minecraft.player == null) {
                return;
            }
            var ctx = new FabricHudContext(graphics, minecraft);
            layers.forEach(layer -> layer.render(ctx));
        });
    }
}
