package lv.cebbys.mcmods.mvd;

import java.util.Optional;

/**
 * One way of finding the Minecraft version. Sources are found with {@link java.util.ServiceLoader}
 * (register them in {@code META-INF/services/lv.cebbys.mcmods.mvd.VersionSource}) and tried by
 * descending {@link #priority()}; the first one that returns a parseable version wins.
 */
public interface VersionSource {
    /** Higher runs first. Loader sources use 1000, {@code version.json} 100, reflection 0. */
    int priority();

    /** Short name shown in logs and errors, e.g. {@code fabric-loader}. */
    String name();

    /**
     * @return the raw version string (e.g. {@code 1.21.1}), or empty when this source does not apply
     * @throws Exception when the source applies but failed; reported if every source fails
     */
    Optional<String> detect() throws Exception;
}
