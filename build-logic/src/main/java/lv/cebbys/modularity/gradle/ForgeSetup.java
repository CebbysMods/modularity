package lv.cebbys.modularity.gradle;

import net.minecraftforge.gradle.ForgeGradleExtension;
import net.minecraftforge.gradle.MinecraftExtensionForProject;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Forge platform with ForgeGradle 7: official mappings, Forge from the Mavenizer repository, nested modules in
 * Forge's jar-in-jar format ({@code META-INF/jarjar}, which ForgeGradle 7 does not write), the Forge builds of the
 * separate mods, smoke runs.
 */
final class ForgeSetup {

    private ForgeSetup() {
    }

    static void apply(Project project, PlatformTable table, PlatformTable.Platform platform, SourceSet main) {
        MinecraftExtensionForProject minecraft = project.getExtensions().getByType(MinecraftExtensionForProject.class);
        ForgeGradleExtension fg = project.getExtensions().getByType(ForgeGradleExtension.class);
        if (platform.parchment() == null) {
            minecraft.mappings("official", platform.minecraft().toString());
        } else {
            // Parchment (parameter names, javadoc) over the official mappings, compile time only. ForgeGradle's own
            // notation: <parchment timestamp>-<minecraft version>.
            ModularityPlatformPlugin.parchmentRepository(project);
            minecraft.mappings("parchment", platform.parchment() + "-" + platform.minecraft());
        }
        minecraft.mavenizer(project.getRepositories());
        project.getRepositories().maven(fg.getForgeMaven());
        project.getRepositories().maven(fg.getMinecraftLibsMaven());

        DependencyHandler deps = project.getDependencies();
        deps.addProvider("implementation", minecraft.dependency("net.minecraftforge:forge:" + platform.minecraft() + "-"
                + platform.loaderVersion()).asProvider());

        // Nested modules: Forge's jar-in-jar (same format NeoForge's ModDevGradle writes).
        Configuration nested = project.getConfigurations().create("modularityNested", c -> {
            c.setCanBeConsumed(false);
            c.setTransitive(false);
        });
        List<String> nestedIds = new ArrayList<>();
        for (String path : table.mod().nested()) {
            deps.add("api", deps.project(Map.of("path", path)));
            deps.add(nested.getName(), deps.project(Map.of("path", path)));
            nestedIds.add(path);
        }
        TaskProvider<JarJarMetadataTask> jarJarMetadata = project.getTasks().register("jarJarMetadata",
                JarJarMetadataTask.class, t -> {
                    t.getEntries().set(project.provider(() -> nestedIds.stream().map(path -> {
                        Project p = project.project(path);
                        return p.getGroup() + ":" + p.getName() + ":" + p.getVersion();
                    }).toList()));
                    t.getOutputDir().set(project.getLayout().getBuildDirectory().dir("generated/jarjar"));
                });
        project.getTasks().named("jar", Jar.class, jar -> {
            jar.from(jarJarMetadata);
            jar.from(nested, spec -> {
                spec.into("META-INF/jarjar");
                spec.rename(name -> nestedJarName(project, nestedIds, name));
            });
        });

        for (String path : table.mod().mods()) {
            deps.add("implementation", deps.project(Map.of("path", NeoForgeSetup.loaderModule(path, platform.loader()))));
            deps.add("compileOnly", deps.project(Map.of("path", NeoForgeSetup.coreModule(path))));
        }

        // Forge's development mod discovery takes each class path folder as its own mod file: a mod's resources
        // (mods.toml) must sit in the same folder as its classes.
        oneOutputDir(main);
        SmokeSetup.apply(project, platform, main, (p, pl, mainSet, smoke, guestMods) -> {
            oneOutputDir(smoke);
            var configs = p.getConfigurations();
            configs.getByName(smoke.getImplementationConfigurationName())
                    .extendsFrom(configs.getByName(mainSet.getImplementationConfigurationName()));
            configs.getByName(smoke.getRuntimeOnlyConfigurationName())
                    .extendsFrom(configs.getByName(mainSet.getRuntimeOnlyConfigurationName()));
            smoke.setRuntimeClasspath(smoke.getOutput().plus(mainSet.getOutput())
                    .plus(configs.getByName(smoke.getRuntimeClasspathConfigurationName())));
            // ForgeGradle fills a run (main class, arguments) from Forge's run configuration of the same name and
            // registers its task per source set: run<Name> for main, run<SourceSet><Name> (runSmokeServer) for smoke,
            // with that source set's class path.
            for (String side : List.of("server", "client")) {
                minecraft.getRuns().register(side, r -> {
                    r.getWorkingDir().set(p.getLayout().getProjectDirectory().dir(SmokeSetup.RUN_DIR));
                    r.args(side.equals("server") ? List.of("--nogui") : SmokeSetup.CLIENT_ARGS);
                    r.getMods().register(table.mod().id(), m -> m.source(mainSet));
                    r.getMods().register("modularity_smoke", m -> m.source(smoke));
                    if (guestMods != null) {
                        r.systemProperty("modularity.guestMods", guestMods);
                    }
                });
            }
            return SmokeSetup.RunTasks.SMOKE;
        });
    }

    private static void oneOutputDir(SourceSet sourceSet) {
        sourceSet.getOutput().setResourcesDir(sourceSet.getJava().getDestinationDirectory().get().getAsFile());
    }

    /** {@code group.artifact-version.jar}, as ModDevGradle names nested jars. */
    private static String nestedJarName(Project project, List<String> paths, String fileName) {
        for (String path : paths) {
            Project p = project.project(path);
            if (fileName.startsWith(p.getName() + "-")) {
                return p.getGroup() + "." + p.getName() + "-" + p.getVersion() + ".jar";
            }
        }
        return fileName;
    }

    /** {@code META-INF/jarjar/metadata.json} for the nested modules ({@code group:artifact:version} each). */
    public abstract static class JarJarMetadataTask extends DefaultTask {
        @Input
        public abstract ListProperty<String> getEntries();

        @OutputDirectory
        public abstract DirectoryProperty getOutputDir();

        @TaskAction
        public void write() {
            List<String> jars = new ArrayList<>();
            for (String entry : getEntries().get()) {
                String[] gav = entry.split(":");
                jars.add("""
                            {
                              "identifier": {"group": "%s", "artifact": "%s"},
                              "version": {"range": "[%s,)", "artifactVersion": "%s"},
                              "path": "META-INF/jarjar/%s.%s-%s.jar",
                              "isObfuscated": false
                            }""".formatted(gav[0], gav[1], gav[2], gav[2], gav[0], gav[1], gav[2]));
            }
            String json = "{\n  \"jars\": [\n" + String.join(",\n", jars) + "\n  ]\n}\n";
            try {
                Path file = getOutputDir().get().getAsFile().toPath().resolve("META-INF/jarjar/metadata.json");
                Files.createDirectories(file.getParent());
                Files.writeString(file, json);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
