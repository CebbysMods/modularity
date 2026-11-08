package lv.cebbys.modularity.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Parent project of one Minecraft version ({@code :platforms:21.1}): the tasks run for its loader projects
 * ({@code :platforms:21.1:21.1-fabric}, ...). Gradle's names, so tooling and habit work:
 * <ul>
 *   <li>{@code build} — Minecraft sources ({@code genSources}), classes, tests and the publishable jars in
 *       {@code build/libs} of every loader platform;</li>
 *   <li>{@code compile} — only the compilation, the fast loop while editing;</li>
 *   <li>{@code clean}, {@code smoke}, plus {@code genSources} and {@code collectJars} as the two build steps,
 *       runnable on their own.</li>
 * </ul>
 * A child without the task is skipped, e.g. {@code genSources}, which only Loom (Fabric and Quilt) provides.
 */
public class ModularityVersionPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        aggregate(project, "compile", "build", "Compiles every loader platform of " + project.getName());
        aggregate(project, "genSources", "modularity",
                "Generates Minecraft sources for the loader platforms of " + project.getName() + " that support it");
        aggregate(project, "smoke", "verification",
                "Runs the smoke tests of every loader platform of " + project.getName());

        project.getTasks().register("clean", Delete.class, task -> {
            task.setGroup("build");
            task.setDescription("Cleans every loader platform of " + project.getName());
            task.delete(project.getLayout().getBuildDirectory());
            task.dependsOn((Callable<Object>) () -> childTasks(project, "clean"));
        });

        // The publishable jar of each platform, in one folder: remapJar for Fabric, jar for NeoForge and Forge
        // (Quilt publishes the Fabric jar, so it has none of its own).
        TaskProvider<Sync> jars = project.getTasks().register("collectJars", Sync.class, task -> {
            task.setGroup("build");
            task.setDescription("Copies the publishable jar of every loader platform to build/libs");
            task.into(project.getLayout().getBuildDirectory().dir("libs"));
            task.from((Callable<Object>) () -> modules(project));
        });

        project.getTasks().register("build", task -> {
            task.setGroup("build");
            task.setDescription("Builds every loader platform of " + project.getName()
                    + ": Minecraft sources, classes, tests and the publishable jars in build/libs");
            task.dependsOn(jars);
            task.dependsOn((Callable<Object>) () -> childTasks(project, "build"));
            // Automatic, but Loom skips genSources while its output is up to date: sources only on the first
            // build of a version, after a Minecraft or mappings change.
            task.dependsOn((Callable<Object>) () -> childTasks(project, "genSources"));
        });

        // Children are registered after this plugin (the root plugin applies it while evaluating the root).
        project.getGradle().projectsEvaluated(gradle -> orderSourcesAfterCompilation(project));
    }

    /**
     * Loom keeps one mapped Minecraft jar per version in the root {@code .gradle/loom-cache}, so the
     * {@code compileJava} of one platform reads a file that the {@code genSourcesWithVineflower} of another (Fabric,
     * Quilt) writes. A parallel build rejects that as an undeclared dependency, so the two are ordered within the
     * version (every version has its own mappings and jar). Sources after compilation, not before: Loom's
     * {@code genSourcesWithVineflower} already depends on its own {@code compileJava}, so the other direction is a
     * cycle. {@code mustRunAfter}, not {@code dependsOn}: compiling does not force decompilation.
     */
    static void orderSourcesAfterCompilation(Project parent) {
        // Paths, not tasks: creating a child's task here would resolve its configurations while the parent is
        // still being configured, which Gradle forbids. The names are read without creating anything.
        List<String> compilations = parent.getChildProjects().values().stream()
                .filter(child -> child.getTasks().getNames().contains("compileJava"))
                .map(child -> child.getPath() + ":compileJava")
                .toList();
        if (compilations.isEmpty()) {
            return;
        }
        for (Project child : parent.getChildProjects().values()) {
            if (child.getTasks().getNames().contains("genSourcesWithVineflower")) {
                // configure, not configureEach: registration is all that happens now, realisation is deferred.
                child.getTasks().named("genSourcesWithVineflower").configure(task -> task.mustRunAfter(compilations));
            }
        }
    }

    /**
     * @param name the task of this project and of the children it depends on; the platform plugin gives the
     *             children the same names, {@code compile} being {@code compileJava} there
     */
    private static void aggregate(Project project, String name, String group, String description) {
        project.getTasks().register(name, task -> {
            task.setGroup(group);
            task.setDescription(description);
            task.dependsOn((Callable<Object>) () -> childTasks(project, name));
        });
    }

    /** Tasks with this name in the direct child projects, skipping projects that do not have one. */
    private static List<Task> childTasks(Project parent, String name) {
        return parent.getChildProjects().values().stream()
                .map(child -> child.getTasks().findByName(name))
                .filter(Objects::nonNull)
                .toList();
    }

    /** The mod jar tasks of the children that publish a jar, read lazily: the table is not needed until then. */
    private static List<Object> modules(Project parent) {
        PlatformTable table = PlatformTable.read(new File(parent.getRootDir(), PlatformTable.FILE).toPath());
        return parent.getChildProjects().values().stream()
                .map(child -> table.platformByProjectName(child.getName()).orElse(null))
                .filter(p -> p != null && p.loader().publishes())
                .map(p -> parent.project(p.projectPath()).getTasks().named(ModularityPlatformPlugin.modJarTask(p)))
                .map(Object.class::cast)
                .toList();
    }
}
