package lv.cebbys.modularity.gradle.smoke;

import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

/**
 * Marker service with one usage at a time: smoke games of different platforms never run in parallel (memory, window,
 * ports), even with {@code org.gradle.parallel=true}.
 */
public abstract class SmokeRunService implements BuildService<BuildServiceParameters.None> {
    public static final String NAME = "modularitySmokeRuns";
}
