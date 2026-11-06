package lv.cebbys.mcmods.modularity.fabric.client;

import lv.cebbys.mcmods.modularity.api.client.ClientState;
import lv.cebbys.mcmods.modularity.api.client.HudContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

record FabricHudContext(GuiGraphics graphics, Minecraft minecraft) implements HudContext {
    @Override
    public int width() {
        return graphics.guiWidth();
    }

    @Override
    public int height() {
        return graphics.guiHeight();
    }

    @Override
    public int textWidth(String text) {
        return minecraft.font.width(text);
    }

    @Override
    public void text(String text, int x, int y, int color) {
        int argb = (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
        graphics.drawString(minecraft.font, text, x, y, argb, true);
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y2, color);
    }

    @Override
    public ClientState client() {
        return FabricClientState.INSTANCE;
    }
}
