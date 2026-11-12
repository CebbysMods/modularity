package lv.cebbys.mcmods.modularity.api.core;

import java.util.Locale;

public enum Loader {
    FABRIC, QUILT, NEOFORGE, FORGE, TEST;

    /** {@code fabric}, {@code quilt}, {@code neoforge}, {@code forge}, {@code test}: the name used in logs and files. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return this.id();
    }
}
