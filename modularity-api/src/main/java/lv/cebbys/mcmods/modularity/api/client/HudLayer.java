package lv.cebbys.mcmods.modularity.api.client;

/** Draws on top of the game (not while a screen is open or the HUD is hidden). */
@FunctionalInterface
public interface HudLayer {
    void render(HudContext ctx);
}
