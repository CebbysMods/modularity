package lv.cebbys.modularity.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.Sync;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Root project: applies {@code lv.cebbys.modularity} to every included platform project and the version plugin to
 * their parents, and registers {@code buildAll}, {@code collectJars} (Sync into {@code build/libs-all}),
 * {@code smokeAll} and {@code allureCollectResults} (Sync into {@code build/allure-results}).
 */
public class ModularityRootPlugin implements Plugin<Project> {

    @Override
    public void apply(Project root) {
        PlatformTable table = PlatformTable.read(new File(root.getRootDir(), PlatformTable.FILE).toPath());

        List<PlatformTable.Platform> included = new ArrayList<>();
        Set<String> versions = new LinkedHashSet<>();
        for (PlatformTable.Platform p : table.platforms()) {
            Project project = root.findProject(p.projectPath());
            // Only projects the settings plugin placed in the layout (older hand-written platform projects
            // with their own build file are left alone).
            if (project != null && project.getProjectDir().toPath().normalize()
                    .equals(new File(root.getRootDir(), p.projectDir()).toPath().normalize())) {
                included.add(p);
                project.getPluginManager().apply(ModularityPlatformPlugin.class);
                if (versions.add(p.parentPath())) {
                    Project parent = root.findProject(p.parentPath());
                    if (parent != null) {
                        parent.getPluginManager().apply(ModularityVersionPlugin.class);
                    }
                }
            }
        }

        root.getTasks().register("buildAll", t -> {
            t.setGroup("build");
            t.setDescription("Builds every project, including all included platforms");
            // Lazily: container projects have no build task.
            t.dependsOn((Callable<Object>) () -> root.getSubprojects().stream()
                    .map(p -> p.getTasks().findByName("build")).filter(java.util.Objects::nonNull).toList());
        });

        root.getTasks().register("collectJars", Sync.class, t -> {
            t.setGroup("build");
            t.setDescription("Copies the universal jar of every Minecraft version to build/libs-all/");
            // Sync, not Copy: removes jars of renamed or removed versions.
            // One jar per version, not one per loader: the parent's jar merges every loader of that version.
            for (String parent : versions) {
                t.from((Callable<Object>) () -> root.project(parent).getTasks().named("jar"));
            }
            t.into(root.getLayout().getBuildDirectory().dir("libs-all"));
        });

        root.getTasks().register("smokeAll", t -> {
            t.setGroup("verification");
            t.setDescription("Runs the smoke tests of every included platform");
            for (String parent : versions) {
                t.dependsOn(parent + ":smoke");
            }
        });

        registerAllureCollectResults(root);
    }

    /**
     * Stages the Allure raw results of every test layer into {@code build/allure-results}, where the aggregate report
     * reads them (docs/07, 7.11). The results are copied, not linked: the aggregate plugin cannot cross an included
     * build, and it must never depend on a smoke task, which launches the game.
     */
    private static void registerAllureCollectResults(Project root) {
        boolean tests = !"false".equalsIgnoreCase(String.valueOf(root.findProperty("modularity.allure.tests")));
        // The L1 unit tests of the two library modules. Their build scripts run after this plugin, so the test
        // tasks are looked up lazily.
        List<Project> modules = List.of(root.project(":modularity-api"), root.project(":modularity-testkit"));
        root.getTasks().register("allureCollectResults", Sync.class, t -> {
            t.setGroup("verification");
            t.setDescription("Stages the Allure results of every test layer into build/allure-results/");
            t.into(root.getLayout().getBuildDirectory().dir("allure-results"));
            // Each project writes its own executor.json; the report does not use it, and one copy would overwrite
            // another. Result and attachment files carry a unique name, so the rest can share one directory.
            t.exclude("executor.json");
            if (tests) {
                for (Project project : modules) {
                    t.from(project.getLayout().getBuildDirectory().dir("allure-results"));
                    t.dependsOn(project.getTasks().named("test"));
                }
            }
        });
    }
}
