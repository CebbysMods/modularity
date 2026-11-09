package lv.cebbys.modularity.gradle.setup;

/**
 * One platform project, configured in fixed stages. Every stage receives its own context record, so a stage can gain
 * an input without any other stage's signature changing.
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
}
