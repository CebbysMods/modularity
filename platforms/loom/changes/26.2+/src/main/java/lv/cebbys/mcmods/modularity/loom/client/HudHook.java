package lv.cebbys.mcmods.modularity.loom.client;

import lv.cebbys.mcmods.modularity.platform.client.McHudContext;
import lv.cebbys.mcmods.modularity.api.client.HudLayer;
import lv.cebbys.mcmods.modularity.platform.compat.Ids;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * Draws the portable HUD layers on top of the vanilla HUD, unless the GUI is hidden or a screen is open. 26.1: one
 * element in Fabric's HUD element registry (the old HUD callback is gone). 26.2: hidden
 * state and open screen live in {@code Minecraft.gui}.
 */
final class HudHook {
    private HudHook() {
    }

    static void install(List<HudLayer> layers) {
        HudElementRegistry.addLast(Ids.of("modularity", "hud_layers"), (graphics, delta) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.gui.hud.isHidden() || minecraft.gui.screen() != null || minecraft.player == null) {
                return;
            }
            var ctx = new McHudContext(graphics, minecraft);
            layers.forEach(layer -> layer.render(ctx));
        });
    }
}
