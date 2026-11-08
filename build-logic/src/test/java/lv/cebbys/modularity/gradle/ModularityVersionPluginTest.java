package lv.cebbys.modularity.gradle;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The aggregate tasks of a version parent project ({@code :platforms:21.1}). */
class ModularityVersionPluginTest {

    @Test
    void aggregatesTheChildTaskOfTheSameJob(@TempDir Path dir) {
        Project root = ProjectBuilder.builder().withProjectDir(dir.toFile()).build();
        Project parent = ProjectBuilder.builder().withParent(root).withName("21.1").build();
        Project fabric = ProjectBuilder.builder().withParent(parent).withName("21.1-fabric").build();
        Project forge = ProjectBuilder.builder().withParent(parent).withName("21.1-forge").build();
        for (Project child : List.of(fabric, forge)) {
            child.getTasks().register("compileJava", t -> {
            });
            child.getTasks().register("build", t -> {
            });
            child.getTasks().register("smoke", t -> {
            });
        }
        // Only Loom (Fabric, Quilt) has genSources; the aggregate must skip the platforms without one.
        fabric.getTasks().register("genSources", t -> {
        });
        parent.getPluginManager().apply(ModularityVersionPlugin.class);

        // compileAll is the only aggregate whose name differs from the child task it runs.
        assertEquals(List.of(":21.1:21.1-fabric:compileJava", ":21.1:21.1-forge:compileJava"),
                dependencies(parent, "compileAll"));
        assertEquals(List.of(":21.1:21.1-fabric:build", ":21.1:21.1-forge:build"), dependencies(parent, "build"));
        assertEquals(List.of(":21.1:21.1-fabric:smoke", ":21.1:21.1-forge:smoke"), dependencies(parent, "smoke"));
        assertEquals(List.of(":21.1:21.1-fabric:genSources"), dependencies(parent, "genSources"));
    }

    private static List<String> dependencies(Project project, String taskName) {
        Task task = project.getTasks().getByName(taskName);
        return task.getTaskDependencies().getDependencies(task).stream().map(Task::getPath).sorted().toList();
    }
}
