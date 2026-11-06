package lv.cebbys.modularity.gradle;

import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

/**
 * Marker service limiting Loom remap tasks of all platforms to {@link #MAX_PARALLEL} at a time: each holds a whole
 * Minecraft mapping tree in the daemon heap, and a cold build of every platform in parallel runs out of memory.
 */
public abstract class RemapService implements BuildService<BuildServiceParameters.None> {
    public static final String NAME = "modularityRemaps";
    public static final int MAX_PARALLEL = 2;
}
