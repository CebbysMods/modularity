package lv.cebbys.modularity.gradle.setup;

import lv.cebbys.modularity.gradle.PlatformTable;
import org.gradle.api.Project;

/** Stage S1b: the common Java setup (publication plugin, archive name, toolchain, compile options). */
record JavaSetupContext(Project project, PlatformTable table, PlatformTable.Platform platform)
        implements PlatformView {

    JavaSetupContext(PlatformSetupContext root) {
        this(root.project(), root.table(), root.platform());
    }
}
