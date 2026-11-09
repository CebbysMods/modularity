package lv.cebbys.modularity.gradle.setup;

import lv.cebbys.modularity.gradle.PlatformTable;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskContainer;

/**
 * One platform project, configured in fixed stages. Every stage receives its own context record, so a stage can gain
 * an input without any other stage's signature changing; the contexts are package private and live here, with the
 * template that owns them.
 *
 * <p>Scaffolding: the stage bodies are still empty; the platform classes (and this base class's shared stages) are
 * filled in by the following commits. The ordering below is the contract.
 */
abstract class AbstractPlatformSetup {

    /** The stages, in order. The only entry point; never overridden. */
    final void apply(PlatformSetupContext root) {
        loaderPlugins(new LoaderPluginsContext(root));
        javaSetup(new JavaSetupContext(root));
        sources(new SourcesContext(root));
        dependencies(new DependenciesContext(root));
        tasks(new TasksContext(root));
        runs(new RunsContext(root));
        publication(new PublicationsContext(root));
    }

    /** S1a: the loader's Gradle plugin(s). */
    protected abstract void loaderPlugins(LoaderPluginsContext ctx);

    /** S1b: the common Java setup: publication plugin, archive name, toolchain, sources jar, compile options. */
    protected void javaSetup(JavaSetupContext ctx) {
    }

    /** S2: the layered sources and the generated mod metadata of the main source set. */
    protected void sources(SourcesContext ctx) {
    }

    /** S3: Minecraft and its mappings, the loader, the APIs, the nested modules, the separate mods. */
    protected abstract void dependencies(DependenciesContext ctx);

    /** S4: the loader-specific task wiring. Not every loader has any. */
    protected void tasks(TasksContext ctx) {
    }

    /** S5: the development and smoke runs. */
    protected abstract void runs(RunsContext ctx);

    /** S6: the {@code maven} publication of the platform jar. */
    protected void publication(PublicationsContext ctx) {
    }

    /** What every stage context reads: the project it configures and the platform it builds. */
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

    /** Everything one platform project is built from; each stage context is built from this. */
    record PlatformSetupContext(Project project, PlatformTable table, PlatformTable.Platform platform, SourceSet main)
            implements PlatformView {
    }

    /** S1a: the loader's Gradle plugin(s) and nothing else. */
    record LoaderPluginsContext(Project project, PlatformTable table, PlatformTable.Platform platform)
            implements PlatformView {

        LoaderPluginsContext(PlatformSetupContext root) {
            this(root.project(), root.table(), root.platform());
        }
    }

    /** S1b: the common Java setup (publication plugin, archive name, toolchain, compile options). */
    record JavaSetupContext(Project project, PlatformTable table, PlatformTable.Platform platform)
            implements PlatformView {

        JavaSetupContext(PlatformSetupContext root) {
            this(root.project(), root.table(), root.platform());
        }
    }

    /** S2: the layered sources and the generated mod metadata of the main source set. */
    record SourcesContext(Project project, PlatformTable table, PlatformTable.Platform platform, SourceSet main)
            implements PlatformView {

        SourcesContext(PlatformSetupContext root) {
            this(root.project(), root.table(), root.platform(), root.main());
        }
    }

    /** S3: Minecraft, the loader, the APIs, the nested modules and the separate mods. */
    record DependenciesContext(Project project, PlatformTable table, PlatformTable.Platform platform, SourceSet main)
            implements PlatformView {

        DependenciesContext(PlatformSetupContext root) {
            this(root.project(), root.table(), root.platform(), root.main());
        }
    }

    /** S4: the loader-specific task wiring (Forge's jar-in-jar, Quilt's launch properties). */
    record TasksContext(Project project, PlatformTable table, PlatformTable.Platform platform, SourceSet main)
            implements PlatformView {

        TasksContext(PlatformSetupContext root) {
            this(root.project(), root.table(), root.platform(), root.main());
        }
    }

    /** S5: the development and smoke runs. */
    record RunsContext(Project project, PlatformTable table, PlatformTable.Platform platform, SourceSet main)
            implements PlatformView {

        RunsContext(PlatformSetupContext root) {
            this(root.project(), root.table(), root.platform(), root.main());
        }
    }

    /** S6: the {@code maven} publication of the platform jar. */
    record PublicationsContext(Project project, PlatformTable table, PlatformTable.Platform platform)
            implements PlatformView {

        PublicationsContext(PlatformSetupContext root) {
            this(root.project(), root.table(), root.platform());
        }
    }
}
