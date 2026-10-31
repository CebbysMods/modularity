package lv.cebbys.mcmods.modularity.api.client;

/** A rebindable key registered by a mod. */
public interface KeyBinding {
    String name();

    boolean isDown();

    /** {@code true} once per press, like vanilla's "consumeClick". */
    boolean consumeClick();
}
