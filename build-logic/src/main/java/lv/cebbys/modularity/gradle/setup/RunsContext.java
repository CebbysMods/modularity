package lv.cebbys.modularity.gradle.setup;

import lv.cebbys.modularity.gradle.PlatformTable;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;

/** Stage S5: the development and smoke runs. */
record RunsContext(Project project, PlatformTable table, PlatformTable.Platform platform, SourceSet main)
        implements PlatformView {

    RunsContext(PlatformSetupContext root) {
        this(root.project(), root.table(), root.platform(), root.main());
    }
}
