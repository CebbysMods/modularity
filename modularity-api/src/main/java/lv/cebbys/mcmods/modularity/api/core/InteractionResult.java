package lv.cebbys.mcmods.modularity.api.core;

/** Outcome of an interaction (using an item, clicking an entity). */
public enum InteractionResult {
    /** Not handled, let others / vanilla try. */
    PASS,
    /** Handled, swing the arm. */
    SUCCESS,
    /** Handled, no arm swing. */
    CONSUME,
    /** Handled and refused. */
    FAIL;

    public boolean handled() {
        return this != PASS;
    }
}
