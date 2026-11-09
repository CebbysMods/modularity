package lv.cebbys.modularity.gradle.setup;

import lv.cebbys.modularity.gradle.PlatformTable;
import org.gradle.api.Project;

/** Stage S6: the {@code maven} publication of the platform jar. */
record PublicationsContext(Project project, PlatformTable table, PlatformTable.Platform platform)
        implements PlatformView {

    PublicationsContext(PlatformSetupContext root) {
        this(root.project(), root.table(), root.platform());
    }
}
