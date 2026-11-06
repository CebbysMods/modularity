package lv.cebbys.modularity.gradle;

import net.neoforged.moddevgradle.dsl.NeoForgeExtension;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.tasks.SourceSet;

import java.util.Map;

/**
 * NeoForge platform with ModDevGradle: NeoForge version, nested modules ({@code jarJar}), the NeoForge builds of the
 * separate mods, the mods and smoke runs of development.
 */
final class NeoForgeSetup {

    private NeoForgeSetup() {
    }

    static void apply(Project project, PlatformTable table, PlatformTable.Platform platform, SourceSet main) {
        NeoForgeExtension neoForge = project.getExtensions().getByType(NeoForgeExtension.class);
        neoForge.setVersion(platform.loaderVersion());

        DependencyHandler deps = project.getDependencies();
        for (String path : table.mod().nested()) {
            deps.add("api", deps.project(Map.of("path", path)));
            deps.add("jarJar", deps.project(Map.of("path", path)));
        }
        for (String path : table.mod().mods()) {
            deps.add("implementation", deps.project(Map.of("path", loaderModule(path, platform.loader()))));
        }

        neoForge.getMods().register(table.mod().id(), m -> m.sourceSet(main));
        SmokeSetup.apply(project, platform, main, (p, pl, mainSet, smoke, guestMods) -> {
            // Minecraft, NeoForge and the dev launcher for the smoke source set (its runs use its class path).
            neoForge.addModdingDependenciesTo(smoke);
            var configs = p.getConfigurations();
            configs.getByName(smoke.getImplementationConfigurationName())
                    .extendsFrom(configs.getByName(mainSet.getImplementationConfigurationName()));
            configs.getByName(smoke.getRuntimeOnlyConfigurationName())
                    .extendsFrom(configs.getByName(mainSet.getRuntimeOnlyConfigurationName()));
            smoke.setRuntimeClasspath(smoke.getOutput().plus(mainSet.getOutput())
                    .plus(configs.getByName(smoke.getRuntimeClasspathConfigurationName())));
            neoForge.getMods().register("modularity_smoke", m -> m.sourceSet(smoke));
            neoForge.getRuns().register("smokeServer", r -> {
                r.server();
                r.getSourceSet().set(smoke);
                r.getGameDirectory().set(p.getLayout().getProjectDirectory().dir(SmokeSetup.RUN_DIR));
                r.getProgramArguments().add("--nogui");
                if (guestMods != null) {
                    r.systemProperty("modularity.guestMods", guestMods);
                }
            });
            neoForge.getRuns().register("smokeClient", r -> {
                r.client();
                r.getSourceSet().set(smoke);
                r.getGameDirectory().set(p.getLayout().getProjectDirectory().dir(SmokeSetup.RUN_DIR));
                r.getProgramArguments().addAll(SmokeSetup.CLIENT_ARGS);
                if (guestMods != null) {
                    r.systemProperty("modularity.guestMods", guestMods);
                }
            });
        });
    }

    /**
     * The loader's build of a separate mod: {@code [mod] mods} lists the Fabric modules ({@code :utilities:mvd:mvd-fabric});
     * NeoForge and Forge use their sibling ({@code mvd-neoforge}, {@code mvd-forge}).
     */
    static String loaderModule(String fabricPath, Loader loader) {
        return fabricPath.replaceAll("-fabric$", "-" + loader.id());
    }
}
