package lv.cebbys.modularity.gradle.setup;

import lv.cebbys.modularity.gradle.PlatformTable;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;

/** Stage S3: Minecraft, the loader, the APIs, the nested modules and the separate mods. */
record DependenciesContext(Project project, PlatformTable table, PlatformTable.Platform platform, SourceSet main)
        implements PlatformView {

    DependenciesContext(PlatformSetupContext root) {
        this(root.project(), root.table(), root.platform(), root.main());
    }
}
