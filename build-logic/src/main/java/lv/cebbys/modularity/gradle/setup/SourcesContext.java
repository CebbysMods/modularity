package lv.cebbys.modularity.gradle.setup;

import lv.cebbys.modularity.gradle.PlatformTable;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;

/** Stage S2: the layered sources and the generated mod metadata of the main source set. */
record SourcesContext(Project project, PlatformTable table, PlatformTable.Platform platform, SourceSet main)
        implements PlatformView {

    SourcesContext(PlatformSetupContext root) {
        this(root.project(), root.table(), root.platform(), root.main());
    }
}
