package lv.cebbys.modularity.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Parent project of one Minecraft version ({@code :platforms:21.1}): tasks that aggregate its loader projects
 * ({@code :platforms:21.1:21.1-fabric}, ...). Tasks that a child does not have are skipped, e.g. {@code genSources},
 * which only Loom (Fabric and Quilt) provides.
 */
public class ModularityVersionPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        aggregate(project, "compileAll", "build", "Compiles every loader platform of " + project.getName());
        aggregate(project, "build", "build", "Builds every loader platform of " + project.getName());
        aggregate(project, "smoke", "verification", "Runs the smoke tests of every loader platform of " + project.getName());
        aggregate(project, "genSources", "modularity",
                "Generates Minecraft sources for the loader platforms of " + project.getName() + " that support it");
    }

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
