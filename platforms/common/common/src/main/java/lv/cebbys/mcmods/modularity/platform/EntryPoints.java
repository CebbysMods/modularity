package lv.cebbys.mcmods.modularity.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One line per loader entry point, so a run proves which of them the loader reached. Deliberately free of every
 * other dependency: it is called before the loader seams are installed.
 */
public final class EntryPoints {
    private static final Logger LOGGER = LoggerFactory.getLogger("modularity");

    private EntryPoints() {
    }

    public static void active(EntryPoint entryPoint, String loader) {
        active(entryPoint.label(), loader);
    }

    /** For a loader entry point that is not one of the four roles, such as the mod constructor. */
    public static void active(String entryPoint, String loader) {
        LOGGER.info("Entrypoint {} active ({})", entryPoint, loader);
    }
}
