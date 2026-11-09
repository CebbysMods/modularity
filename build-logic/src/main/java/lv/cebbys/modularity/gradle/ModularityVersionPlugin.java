package lv.cebbys.modularity.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Parent project of one Minecraft version ({@code :platforms:21.1}): the tasks run for its loader projects
 * ({@code :platforms:21.1:21.1-fabric}, ...). Gradle's names, so tooling and habit work:
 * <ul>
 *   <li>{@code build} — classes and tests of every loader platform, then {@code jar}: the one universal jar of the
 *       version in {@code build/libs}, loadable by every loader;</li>
 *   <li>{@code compile} — only the compilation, the fast loop while editing;</li>
 *   <li>{@code clean}, {@code smoke}, {@code genSources} (Minecraft sources, one decompilation for the version) and
 *       {@code jar}, each runnable on its own.</li>
 * </ul>
 * A child without the task is skipped, e.g. {@code genSources}, which only Loom (Fabric and Quilt) provides.
 */
public class ModularityVersionPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        aggregate(project, "compile", "build", "Compiles every loader platform of " + project.getName());
        aggregate(project, "smoke", "verification",
                "Runs the smoke tests of every loader platform of " + project.getName());

        // One decompilation per version, not one per Loom platform: they share the mappings and Loom's maven
        // directory (orderBuildAfterSources explains why that has to be a single one).
        project.getTasks().register("genSources", task -> {
            task.setGroup("modularity");
            task.setDescription("Generates the Minecraft sources of " + project.getName()
                    + "; the mappings are the same for every loader of the version");
            task.dependsOn((Callable<Object>) () -> {
                Project source = sourcePlatform(project);
                return source == null ? List.of() : List.of(source.getTasks().named("genSources"));
            });
        });

        project.getTasks().register("clean", Delete.class, task -> {
            task.setGroup("build");
            task.setDescription("Cleans every loader platform of " + project.getName());
            task.delete(project.getLayout().getBuildDirectory());
            task.dependsOn((Callable<Object>) () -> childTasks(project, "clean"));
        });

        // One jar for every loader: the platform jars of this version merged, each class set relocated.
        File tableFile = new File(project.getRootDir(), PlatformTable.FILE);
        TaskProvider<UniversalJarTask> universal = project.getTasks().register("jar", UniversalJarTask.class, task -> {
            task.setGroup("build");
            task.setDescription("One jar for every loader of " + project.getName()
                    + ": build/libs/<archive-prefix>-<minecraft>-<modversion>.jar");
            task.getTableFile().set(tableFile);
            task.getPlatform().set(project.getName());
            // The name is known only after the root has set the version: modularity-1.21.1-<version>.jar.
            task.getOutputJar().set(project.getLayout().file(project.provider(() -> {
                PlatformTable table = PlatformTable.read(tableFile.toPath());
                PlatformTable.Platform platform = table.version(project.getName())
                        .orElseThrow(() -> new IllegalStateException("No Minecraft version " + project.getName()
                                + " in " + PlatformTable.FILE));
                return project.getLayout().getBuildDirectory()
                        .file("libs/" + table.mod().archivePrefix() + "-" + platform.minecraft() + "-"
                                + project.getVersion() + ".jar")
                        .get().getAsFile();
            })));
        });

        project.getTasks().register("build", task -> {
            task.setGroup("build");
            task.setDescription("Builds every loader platform of " + project.getName()
                    + ": classes, tests and the universal jar in build/libs");
            task.dependsOn(universal);
            task.dependsOn((Callable<Object>) () -> childTasks(project, "build"));
            // No genSources: Gradle deletes the shared loom-cache Maven directory before decompiling (Loom declares
            // it as an output), which breaks every other Loom project reading it in the same parallel build. The
            // sources are a separate, occasional step: ./gradlew :platforms:<version>:genSources
        });

        // Children are registered after this plugin (the root plugin applies it while evaluating the root).
        project.getGradle().projectsEvaluated(gradle -> {
            orderBuildAfterSources(project);
            universal.configure(task -> wirePlatformJars(task, project, tableFile));
        });
    }

    /**
     * Feeds the jar of every loader platform of the version into the universal jar, which loader each one is taken
     * from the table. Lazy sources and {@code named}, so no child task is created here.
     */
    static void wirePlatformJars(UniversalJarTask task, Project parent, File tableFile) {
        PlatformTable table = PlatformTable.read(tableFile.toPath());
        for (Project child : parent.getChildProjects().values()) {
            table.platformByProjectName(child.getName()).ifPresent(p -> {
                var jars = switch (p.loader()) {
                    case FABRIC -> task.getFabricJar();
                    case FORGE -> task.getForgeJar();
                    case NEOFORGE -> task.getNeoForgeJar();
                    // Quilt runs the Fabric jar's code: it contributes no classes of its own.
                    case QUILT -> null;
                };
                if (jars != null) {
                    jars.from(child.getTasks().named(ModularityPlatformPlugin.modJarTask(p)));
                }
            });
        }
    }

    /**
     * Loom writes one Maven directory per Minecraft version into the root {@code .gradle/loom-cache} and declares it
     * an output of {@code genSourcesWithVineflower}; Gradle deletes declared outputs before running a task. If a
     * decompilation runs in the same invocation as the other platforms of the version, it would delete files they
     * read, so their tasks wait for it. {@code mustRunAfter} only orders tasks that are both scheduled, and
     * {@code build} does not schedule a decompilation at all.
     * <p>
     * Siblings only: the utilities are compile dependencies of the platforms, so ordering them would cycle (the
     * decompilation itself depends on their jars). Run {@code genSources} on its own.
     */
    static void orderBuildAfterSources(Project parent) {
        Project source = sourcePlatform(parent);
        if (source == null) {
            return;
        }
        String decompilation = source.getPath() + ":genSourcesWithVineflower";
        for (Project child : parent.getChildProjects().values()) {
            if (child.getPath().equals(source.getPath())) {
                // Its own tasks are ordered by the dependency of the decompilation on them.
                continue;
            }
            // Every task, not a list of names: which Loom tasks read the Maven directory (compileJava, remapJar,
            // remapSourcesJar, ...) is Loom's business.
            child.getTasks().configureEach(task -> task.mustRunAfter(decompilation));
        }
    }

    /**
     * The child whose Loom decompilation produces the version's shared Minecraft sources: Fabric, else the first
     * Loom platform, or {@code null} for a version without one.
     */
    static Project sourcePlatform(Project parent) {
        Project first = null;
        for (Project child : parent.getChildProjects().values()) {
            if (!child.getTasks().getNames().contains("genSourcesWithVineflower")) {
                continue;
            }
            if (child.getName().endsWith("-" + Loader.FABRIC.id())) {
                return child;
            }
            if (first == null) {
                first = child;
            }
        }
        return first;
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

}
