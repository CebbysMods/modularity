package lv.cebbys.modularity.gradle.setup;

import lv.cebbys.modularity.gradle.PlatformTable;
import lv.cebbys.modularity.gradle.smoke.SmokeSetup;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.tasks.SourceSet;

import java.util.Map;

/**
 * NeoForge and Forge: a Java library with the loader's plugin, the loader's build of the separate mods, the nested
 * modules packaged inside the jar, and the smoke runs registered per source set.
 *
 * <p>The two loaders share conventions, not a tool: how Minecraft and its mappings are obtained, how nested modules
 * are packaged and how runs are registered are hooks, because ModDevGradle and ForgeGradle have nothing in common
 * there.
 */
abstract class AbstractForgeFamilyPlatformSetup extends AbstractPlatformSetup {

    /** S1a: {@code net.neoforged.moddev} or {@code net.minecraftforge.gradle}, applied over {@code java-library}. */
    protected abstract String loaderPluginId();

    @Override
    protected final void loaderPlugins(LoaderPluginsContext ctx) {
        ctx.project().getPluginManager().apply("java-library");
        ctx.project().getPluginManager().apply(loaderPluginId());
    }

    /** S3: the loader's Minecraft, mappings and repositories, then the parts both loaders share. */
    @Override
    protected final void dependencies(DependenciesContext ctx) {
        prepareSourceSet(ctx.main());
        loaderDependencies(ctx);

        // Nested modules: packaged inside the platform jar, and compiled against like any other API.
        DependencyHandler deps = ctx.dependencies();
        for (String path : ctx.mod().nested()) {
            deps.add("api", deps.project(Map.of("path", path)));
        }
        packageNestedModules(ctx);
        companionMods(ctx);
    }

    /** S5: the smoke runs, on a source set that sees the main one. */
    @Override
    protected final void runs(RunsContext ctx) {
        SmokeSetup.apply(ctx.project(), ctx.platform(), ctx.main(), (project, _, main, smoke, guestMods) -> {
            prepareSourceSet(smoke);
            smokeClasspath(project, main, smoke);
            smokeDependencies(project, smoke);
            return registerSmokeRuns(ctx, smoke, guestMods);
        });
    }

    /** The loader's Minecraft, its mappings, its repositories and the loader itself. */
    protected abstract void loaderDependencies(DependenciesContext ctx);

    /** How the nested modules reach the jar: ModDevGradle's {@code jarJar}, Forge's own jar-in-jar. */
    protected abstract void packageNestedModules(DependenciesContext ctx);

    /** The loader's two game runs, in {@link SmokeSetup#RUN_DIR}. */
    protected abstract SmokeSetup.RunTasks registerSmokeRuns(RunsContext ctx, SourceSet smoke, String guestMods);

    /** Before a source set's dependencies are declared: Forge's development mod discovery needs one output dir. */
    protected void prepareSourceSet(SourceSet sourceSet) {
    }

    /** The smoke source set's own loader dependencies (NeoForge: the modding dependencies of the dev launcher). */
    protected void smokeDependencies(Project project, SourceSet smoke) {
    }

    /** The {@code implementation}/{@code compileOnly} pair per companion mod, as built by the loader. */
    private static void companionMods(DependenciesContext ctx) {
        DependencyHandler deps = ctx.dependencies();
        for (String companion : ctx.mod().companionMods()) {
            deps.add("implementation", deps.project(Map.of("path", companionProject(ctx.project(),
                    PlatformTable.Mod.companionProject(companion, ctx.platform().loader())))));
            deps.add("compileOnly", deps.project(Map.of("path",
                    companionProject(ctx.project(), PlatformTable.Mod.companionApi(companion)))));
        }
    }

    /** The smoke source set compiles and runs against the main one's output and dependencies. */
    private static void smokeClasspath(Project project, SourceSet main, SourceSet smoke) {
        var configs = project.getConfigurations();
        configs.getByName(smoke.getImplementationConfigurationName())
                .extendsFrom(configs.getByName(main.getImplementationConfigurationName()));
        configs.getByName(smoke.getRuntimeOnlyConfigurationName())
                .extendsFrom(configs.getByName(main.getRuntimeOnlyConfigurationName()));
        smoke.setRuntimeClasspath(smoke.getOutput().plus(main.getOutput())
                .plus(configs.getByName(smoke.getRuntimeClasspathConfigurationName())));
    }
}
