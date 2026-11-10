package lv.cebbys.mcmods.modularity.api.detect;

import java.util.List;

/**
 * Thrown when no {@link VersionSource} could find the Minecraft version. Lists every source's
 * failure.
 */
public final class VersionDetectionException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    static final String ISSUES = "https://github.com/CebbysMods/modularity/issues";

    private final List<String> failures;

    VersionDetectionException(List<String> failures) {
        super(message(failures));
        this.failures = List.copyOf(failures);
    }

    /** One entry per source, {@code "<source>: <reason>"}, in the order they were tried. */
    public List<String> failures() {
        return failures;
    }

    private static String message(List<String> failures) {
        String reasons = failures.isEmpty() ? "no version sources are registered" : String.join("; ", failures);
        return "Modularity could not find the Minecraft version [" + reasons
                + "]. Please report this at " + ISSUES;
    }
}
