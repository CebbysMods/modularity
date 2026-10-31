package lv.cebbys.mcmods.modularity.api.client;

/** State of the vanilla movement keys this tick. */
public record MovementInput(boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean sneak) {
    public static final MovementInput NONE = new MovementInput(false, false, false, false, false, false);
}
