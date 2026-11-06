package lv.cebbys.modularity.gradle;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;

/** Forge platform with ForgeGradle 7. */
final class ForgeSetup {

    private ForgeSetup() {
    }

    static void apply(Project project, PlatformTable table, PlatformTable.Platform platform, SourceSet main) {
        throw new GradleException(platform.id() + ": Forge platforms are not supported yet");
    }
}
