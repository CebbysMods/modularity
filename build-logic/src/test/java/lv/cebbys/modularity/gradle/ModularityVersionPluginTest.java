package lv.cebbys.modularity.gradle;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The tasks of a version parent project ({@code :platforms:21.1}). */
class ModularityVersionPluginTest {

    private static Project withChildren(Path dir) {
        Project root = ProjectBuilder.builder().withProjectDir(dir.toFile()).build();
        Project parent = ProjectBuilder.builder().withParent(root).withName("21.1").build();
        Project fabric = ProjectBuilder.builder().withParent(parent).withName("21.1-fabric").build();
        Project forge = ProjectBuilder.builder().withParent(parent).withName("21.1-forge").build();
        for (Project child : List.of(fabric, forge)) {
            for (String task : List.of("compile", "compileJava", "build", "clean", "smoke")) {
                child.getTasks().register(task, t -> {
                });
            }
        }
        // Only Loom (Fabric, Quilt) has genSources; the aggregates must skip the platforms without one.
        fabric.getTasks().register("genSources", t -> {
        });
        fabric.getTasks().register("genSourcesWithVineflower", t -> {
        });
        parent.getPluginManager().apply(ModularityVersionPlugin.class);
        return parent;
    }

    @Test
    void compileIsTheFastLoop(@TempDir Path dir) {
        Project parent = withChildren(dir);
        // Same name on the parent and the platforms (the platform plugin's compile runs compileJava).
        assertEquals(List.of(":21.1:21.1-fabric:compile", ":21.1:21.1-forge:compile"),
                dependencies(parent, "compile"));
    }

    @Test
    void buildIsSourcesClassesTestsAndJars(@TempDir Path dir) {
        Project parent = withChildren(dir);
        assertEquals(List.of(":21.1:21.1-fabric:build", ":21.1:21.1-fabric:genSources", ":21.1:21.1-forge:build",
                ":21.1:collectJars"), dependencies(parent, "build"));
    }

    @Test
    void cleanAndSmokeFanOut(@TempDir Path dir) {
        Project parent = withChildren(dir);
        assertEquals(List.of(":21.1:21.1-fabric:clean", ":21.1:21.1-forge:clean"), dependencies(parent, "clean"));
        assertEquals(List.of(":21.1:21.1-fabric:smoke", ":21.1:21.1-forge:smoke"), dependencies(parent, "smoke"));
        assertEquals(List.of(":21.1:21.1-fabric:genSources"), dependencies(parent, "genSources"));
    }

    @Test
    void sourcesOfOnePlatformFollowCompilationOfAnother(@TempDir Path dir) {
        Project parent = withChildren(dir);
        ModularityVersionPlugin.orderSourcesAfterCompilation(parent);
        // Loom's shared loom-cache: every genSourcesWithVineflower waits for every compileJava of the version.
        Task sources = parent.getChildProjects().get("21.1-fabric").getTasks().getByName("genSourcesWithVineflower");
        assertEquals(List.of(":21.1:21.1-fabric:compileJava", ":21.1:21.1-forge:compileJava"),
                sources.getMustRunAfter().getDependencies(sources).stream().map(Task::getPath).sorted().toList());
    }

    private static List<String> dependencies(Project project, String taskName) {
        Task task = project.getTasks().getByName(taskName);
        return task.getTaskDependencies().getDependencies(task).stream().map(Task::getPath).sorted().toList();
    }
}
