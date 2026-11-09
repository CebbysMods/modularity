package lv.cebbys.modularity.gradle.setup;

import lv.cebbys.modularity.gradle.PlatformTable;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;

/**
 * Everything one platform project is built from, as handed to {@link PlatformSetup}. Each stage of
 * {@link AbstractPlatformSetup} takes its own record built from this.
 */
public record PlatformSetupContext(
        Project project,
        PlatformTable table,
        PlatformTable.Platform platform,
        SourceSet main
) implements PlatformView {
}
