package lv.cebbys.mcmods.modularity.platform;

import java.util.Locale;

/** The entrypoint roles of Modularity, the same names on every loader. */
public enum EntryPoint {
    COMMON, CLIENT, SERVER, LAUNCH;

    /** The lowercase name used in the entrypoint log line. */
    public String label() {
        return name().toLowerCase(Locale.ROOT);
    }
}
