package lv.cebbys.modularity.gradle.setup;

import lv.cebbys.modularity.gradle.PlatformTable;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.tasks.TaskContainer;

/**
 * What every stage of a platform setup reads: the project it configures and the platform it builds. Each hook has
 * its own context record implementing this, so a stage only declares the values it actually needs.
 */
interface PlatformView {
    Project project();

    PlatformTable table();

    PlatformTable.Platform platform();

    /** The one mod of the table, for the stages that generate its metadata. */
    default PlatformTable.Mod mod() {
        return table().mod();
    }

    default DependencyHandler dependencies() {
        return project().getDependencies();
    }

    default RepositoryHandler repositories() {
        return project().getRepositories();
    }

    default TaskContainer tasks() {
        return project().getTasks();
    }
}
