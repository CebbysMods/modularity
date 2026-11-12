package lv.cebbys.modularity.gradle;

import lv.cebbys.modularity.gradle.setup.PlatformSetup;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.io.File;

/**
 * One platform project ({@code :platforms:<name>:<name>-<loader>}), configured from {@code gradle/platforms.toml}.
 * The table names the platform; everything a loader does is in its setup in {@code gradle.setup}.
 */
public class ModularityPlatformPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        File tableFile = new File(project.getRootDir(), PlatformTable.FILE);
        PlatformTable table = PlatformTable.read(tableFile.toPath());
        PlatformTable.Platform platform = table.platformByProjectName(project.getName())
                .orElseThrow(() -> new GradleException(project.getPath() + " is not in " + PlatformTable.FILE));
        new PlatformSetup().apply(project, table, platform);
    }
}
