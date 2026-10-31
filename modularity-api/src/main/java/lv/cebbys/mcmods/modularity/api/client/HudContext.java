package lv.cebbys.mcmods.modularity.api.client;

/** 2D drawing in GUI-scaled pixels. Colors are {@code 0xAARRGGBB} ({@code 0xRRGGBB} = opaque). */
public interface HudContext {
    int width();

    int height();

    int textWidth(String text);

    void text(String text, int x, int y, int color);

    void fill(int x1, int y1, int x2, int y2, int color);

    ClientState client();
}
