package lv.cebbys.modularity.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Sync;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Root project: applies {@code lv.cebbys.modularity} to every included platform project and registers
 * {@code buildAll}, {@code collectJars} (Sync into {@code build/libs-all}) and {@code smokeAll}.
 */
public class ModularityRootPlugin implements Plugin<Project> {

    /** Root extension {@code modularity}. */
    public abstract static class Extension {
        /** Further jars for {@code collectJars}, as {@code <project path>:<task>}, e.g. the mvd and mpd jars. */
        public abstract ListProperty<String> getExtraJars();
    }

    @Override
    public void apply(Project root) {
        PlatformTable table = PlatformTable.read(new File(root.getRootDir(), PlatformTable.FILE).toPath());
        Extension ext = root.getExtensions().create("modularity", Extension.class);

        List<PlatformTable.Platform> included = new ArrayList<>();
        for (PlatformTable.Platform p : table.platforms()) {
            Project project = root.findProject(p.projectPath());
            // Only projects the settings plugin placed in the layout (older hand-written platform projects
            // with their own build file are left alone).
            if (project != null && project.getProjectDir().toPath().normalize()
                    .equals(new File(root.getRootDir(), p.projectDir()).toPath().normalize())) {
                included.add(p);
                project.getPluginManager().apply(ModularityPlatformPlugin.class);
            }
        }

        root.getTasks().register("buildAll", t -> {
            t.setGroup("build");
            t.setDescription("Builds every project, including all included platforms");
            // Lazily: container projects (e.g. :utilities) have no build task.
            t.dependsOn((Callable<Object>) () -> root.getSubprojects().stream()
                    .map(p -> p.getTasks().findByName("build")).filter(java.util.Objects::nonNull).toList());
        });

        root.getTasks().register("collectJars", Sync.class, t -> {
            t.setGroup("build");
            t.setDescription("Copies every platform jar and the extra jars (mvd, mpd) to build/libs-all/");
            // Sync, not Copy: removes jars of renamed or removed platforms.
            for (PlatformTable.Platform p : included.stream().filter(p -> p.loader().publishes()).toList()) {
                t.from((Callable<Object>) () -> root.project(p.projectPath()).getTasks()
                        .named(ModularityPlatformPlugin.modJarTask(p)));
            }
            t.from((Callable<Object>) () -> ext.getExtraJars().get().stream().map(s -> {
                int i = s.lastIndexOf(':');
                return root.project(s.substring(0, i)).getTasks().named(s.substring(i + 1));
            }).toList());
            t.into(root.getLayout().getBuildDirectory().dir("libs-all"));
        });

        root.getTasks().register("smokeAll", t -> {
            t.setGroup("verification");
            t.setDescription("Runs the smoke tests of every included platform");
            for (PlatformTable.Platform p : included) {
                t.dependsOn(p.projectPath() + ":smoke");
            }
        });
    }
}
