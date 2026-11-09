package lv.cebbys.modularity.gradle.setup;

import lv.cebbys.modularity.gradle.PlatformTable;
import org.gradle.api.Project;

/** Stage S1a: the loader's Gradle plugin(s) and nothing else. */
record LoaderPluginsContext(Project project, PlatformTable table, PlatformTable.Platform platform)
        implements PlatformView {

    LoaderPluginsContext(PlatformSetupContext root) {
        this(root.project(), root.table(), root.platform());
    }
}
