package lv.cebbys.modularity.gradle.setup;

import lv.cebbys.modularity.gradle.smoke.SmokeSetup;
import net.neoforged.moddevgradle.dsl.NeoForgeExtension;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.tasks.SourceSet;

import java.util.Map;

/**
 * NeoForge platform with ModDevGradle: NeoForge version, Parchment over the official mappings, nested modules in the
 * {@code jarJar} configuration, the NeoForge builds of the separate mods, the smoke runs on the mod dev launcher.
 */
class NeoForgePlatformSetup extends AbstractForgeFamilyPlatformSetup {

    @Override
    protected String loaderPluginId() {
        return "net.neoforged.moddev";
    }

    @Override
    protected void loaderDependencies(DependenciesContext ctx) {
        NeoForgeExtension neoForge = neoForge(ctx.project());
        neoForge.setVersion(ctx.platform().loaderVersion());
        // Parchment (parameter names, javadoc) over the official mappings, compile time only.
        if (ctx.platform().parchment() != null) {
            parchmentRepository(ctx.project());
            neoForge.parchment(p -> {
                p.getMinecraftVersion().set(ctx.platform().minecraft().toString());
                p.getMappingsVersion().set(ctx.platform().parchment());
            });
        }
        neoForge.getMods().register(ctx.mod().id(), m -> m.sourceSet(ctx.main()));
    }

    @Override
    protected void packageNestedModules(DependenciesContext ctx) {
        DependencyHandler deps = ctx.dependencies();
        for (String path : ctx.mod().nested()) {
            deps.add("jarJar", deps.project(Map.of("path", path)));
        }
    }

    @Override
    protected void smokeDependencies(Project project, SourceSet smoke) {
        // Minecraft, NeoForge and the dev launcher for the smoke source set (its runs use its class path).
        NeoForgeExtension neoForge = neoForge(project);
        neoForge.addModdingDependenciesTo(smoke);
        neoForge.getMods().register("modularity_smoke", m -> m.sourceSet(smoke));
    }

    @Override
    protected SmokeSetup.RunTasks registerSmokeRuns(RunsContext ctx, SourceSet smoke, String guestMods) {
        Project project = ctx.project();
        NeoForgeExtension neoForge = neoForge(project);
        neoForge.getRuns().register("smokeServer", r -> {
            r.server();
            r.getSourceSet().set(smoke);
            r.getGameDirectory().set(project.getLayout().getProjectDirectory().dir(SmokeSetup.RUN_DIR));
            r.getProgramArguments().add("--nogui");
            if (guestMods != null) {
                r.systemProperty("modularity.guestMods", guestMods);
            }
        });
        neoForge.getRuns().register("smokeClient", r -> {
            r.client();
            r.getSourceSet().set(smoke);
            r.getGameDirectory().set(project.getLayout().getProjectDirectory().dir(SmokeSetup.RUN_DIR));
            r.getProgramArguments().addAll(SmokeSetup.CLIENT_ARGS);
            if (guestMods != null) {
                r.systemProperty("modularity.guestMods", guestMods);
            }
        });
        return SmokeSetup.RunTasks.SMOKE;
    }

    private static NeoForgeExtension neoForge(Project project) {
        return project.getExtensions().getByType(NeoForgeExtension.class);
    }
}
