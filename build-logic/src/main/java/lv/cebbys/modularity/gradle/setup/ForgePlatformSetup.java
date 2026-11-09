package lv.cebbys.modularity.gradle.setup;

import org.gradle.api.tasks.SourceSet;

/**
 * Forge platform with ForgeGradle 7: Minecraft with official or Parchment mappings from the Mavenizer, the nested
 * modules in Forge's jar-in-jar format ({@code META-INF/jarjar}), the Forge builds of the separate mods, the smoke
 * runs.
 */
public class ForgePlatformSetup extends AbstractPlatformSetup {

    @Override
    protected void loaderPlugins(LoaderPluginsContext ctx) {
    }

    @Override
    protected void dependencies(DependenciesContext ctx) {
    }

    @Override
    protected void tasks(TasksContext ctx) {
        // oneOutputDir(main), jarJarMetadata, the jar task's META-INF/jarjar contents.
    }

    @Override
    protected void runs(RunsContext ctx) {
        // oneOutputDir(smoke), the ForgeGradle runs of the main and the smoke source set.
    }

    /** Forge's development mod discovery reads each class path folder as its own mod: classes and resources together. */
    private static void oneOutputDir(SourceSet sourceSet) {
    }
}
