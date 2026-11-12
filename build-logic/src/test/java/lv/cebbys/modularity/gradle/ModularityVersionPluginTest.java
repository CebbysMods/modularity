package lv.cebbys.modularity.gradle;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import lv.cebbys.modularity.gradle.allure.Functionality;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static lv.cebbys.modularity.gradle.allure.BuildAllureLabels.Epics;
import static lv.cebbys.modularity.gradle.allure.BuildAllureLabels.Features;
import static lv.cebbys.modularity.gradle.allure.BuildAllureLabels.Functionalities;
import static lv.cebbys.modularity.gradle.allure.BuildAllureLabels.Issues;
import static lv.cebbys.modularity.gradle.allure.BuildAllureLabels.Stories;
import static lv.cebbys.modularity.gradle.allure.BuildAllureLabels.Tags;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The tasks of a version parent project ({@code :platforms:21.1}). */
@Epic(Epics.MODULARITY)
@Feature(Features.INTEGRATION)
@Story(Stories.VERSION_PLUGIN)
@Issue(Issues.PR_6)
@Tag(Tags.PR_6)
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
    @Functionality(Functionalities.TASK_GRAPH)
    @DisplayName("compile builds the classes and the jars")
    void compileIsTheFastLoop(@TempDir Path dir) {
        Project parent = withChildren(dir);
        // Same name on the parent and the platforms (the platform plugin's compile runs compileJava).
        assertEquals(List.of(":21.1:21.1-fabric:compile", ":21.1:21.1-forge:compile"),
                dependencies(parent, "compile"));
    }

    @Test
    @Functionality(Functionalities.TASK_GRAPH)
    @DisplayName("build runs the classes, the tests and the jars")
    void buildIsClassesTestsAndJars(@TempDir Path dir) {
        Project parent = withChildren(dir);
        // One jar for the whole version, built from the platform jars. Sources are not part of it: the shared
        // loom-cache makes decompiling and building in one parallel run unsafe (see genSources).
        assertEquals(List.of(":21.1:21.1-fabric:build", ":21.1:21.1-forge:build", ":21.1:jar"),
                dependencies(parent, "build"));
    }

    @Test
    @Functionality(Functionalities.TASK_GRAPH)
    @DisplayName("clean and smoke fan out to every platform")
    void cleanAndSmokeFanOut(@TempDir Path dir) {
        Project parent = withChildren(dir);
        assertEquals(List.of(":21.1:21.1-fabric:clean", ":21.1:21.1-forge:clean"), dependencies(parent, "clean"));
        assertEquals(List.of(":21.1:21.1-fabric:smoke", ":21.1:21.1-forge:smoke"), dependencies(parent, "smoke"));
        assertEquals(List.of(":21.1:21.1-fabric:genSources"), dependencies(parent, "genSources"));
    }

    @Test
    @DisplayName("The tasks of one platform wait for the decompilation of another")
    void tasksOfOnePlatformWaitForTheDecompilationOfAnother(@TempDir Path dir) {
        Project parent = withChildren(dir);
        ModularityVersionPlugin.orderBuildAfterSources(parent);
        // Loom's shared loom-cache: the decompilation deletes and rewrites the Maven directory every other platform
        // of the version reads, so their tasks wait for it.
        for (String name : List.of("compileJava", "build")) {
            Task task = parent.getChildProjects().get("21.1-forge").getTasks().getByName(name);
            assertEquals(List.of(":21.1:21.1-fabric:genSourcesWithVineflower"),
                    task.getMustRunAfter().getDependencies(task).stream().map(Task::getPath).sorted().toList());
        }
        // The platform that decompiles is ordered by its own dependency chain: ordering it would cycle.
        Task compile = parent.getChildProjects().get("21.1-fabric").getTasks().getByName("compileJava");
        assertTrue(compile.getMustRunAfter().getDependencies(compile).isEmpty());
    }

    private static List<String> dependencies(Project project, String taskName) {
        Task task = project.getTasks().getByName(taskName);
        return task.getTaskDependencies().getDependencies(task).stream().map(Task::getPath).sorted().toList();
    }
}
