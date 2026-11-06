package lv.cebbys.mcmods.mpd;

import java.util.List;

/** Thrown when no {@link PlatformSource} could find the platform. Lists every source's failure. */
public final class PlatformDetectionException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    static final String ISSUES = "https://github.com/CebbysMods/modularity/issues";

    private final List<String> failures;

    PlatformDetectionException(List<String> failures) {
        super(message(failures));
        this.failures = List.copyOf(failures);
    }

    /** One entry per source, {@code "<source>: <reason>"}, in the order they were tried. */
    public List<String> failures() {
        return failures;
    }

    private static String message(List<String> failures) {
        String reasons = failures.isEmpty() ? "no platform sources are registered" : String.join("; ", failures);
        return "Minecraft Platform Detector could not find the platform [" + reasons
                + "]. Please report this at " + ISSUES;
    }
}
