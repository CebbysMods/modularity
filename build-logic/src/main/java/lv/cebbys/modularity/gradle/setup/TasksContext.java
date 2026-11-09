package lv.cebbys.modularity.gradle.setup;

import lv.cebbys.modularity.gradle.PlatformTable;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;

/** Stage S4: the loader-specific task wiring (Forge's jar-in-jar, Quilt's launch properties). */
record TasksContext(Project project, PlatformTable table, PlatformTable.Platform platform, SourceSet main)
        implements PlatformView {

    TasksContext(PlatformSetupContext root) {
        this(root.project(), root.table(), root.platform(), root.main());
    }
}
