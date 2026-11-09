package lv.cebbys.modularity.gradle;

import groovy.lang.Closure;
import net.minecraftforge.gradle.ForgeGradleExtension;
import net.minecraftforge.gradle.MavenizerInstance;
import net.minecraftforge.gradle.MinecraftExtensionForProject;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.Attribute;
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
 * Forge platform with ForgeGradle 7: official Mojang mappings (layered with Parchment where the table has a version
 * for the Minecraft version), Forge from the Mavenizer repository, nested modules in Forge's jar-in-jar format
 * ({@code META-INF/jarjar}, which ForgeGradle 7 does not write), the Forge builds of the separate mods, smoke runs.
 */
final class ForgeSetup {

    /** The attributes ForgeGradle's Mavenizer puts on the variants of a Forge dependency (ForgeAttributes). */
    private static final Attribute<String> MAPPINGS_CHANNEL =
            Attribute.of("net.minecraftforge.mappings.channel", String.class);
    private static final Attribute<String> MAPPINGS_VERSION =
            Attribute.of("net.minecraftforge.mappings.version", String.class);

    private ForgeSetup() {
    }

    static void apply(Project project, PlatformTable table, PlatformTable.Platform platform, SourceSet main) {
        MinecraftExtensionForProject minecraft = project.getExtensions().getByType(MinecraftExtensionForProject.class);
        ForgeGradleExtension fg = project.getExtensions().getByType(ForgeGradleExtension.class);
        String parchment = platform.parchment();
        if (parchment == null) {
            minecraft.mappings("official", platform.minecraft().toString());
        } else {
            ModularityPlatformPlugin.parchmentRepository(project);
            // ForgeGradle sanitizes the version to <timestamp>-<minecraft> (ParchmentVersion.toFriendly()).
            minecraft.mappings("parchment", parchment + "-" + platform.minecraft());
        }
        minecraft.mavenizer(project.getRepositories());
        project.getRepositories().maven(fg.getForgeMaven());
        project.getRepositories().maven(fg.getMinecraftLibsMaven());

        DependencyHandler deps = project.getDependencies();
        String coordinates = "net.minecraftforge:forge:" + platform.minecraft() + "-" + platform.loaderVersion();
        // ForgeGradle resolves the Minecraft dependency for the run class path in an attribute-less detached
        // configuration; with only the official variants it resolves, with Parchment variants next to them every
        // variant matches and resolution fails ("Cannot choose between the available variants"). Requesting the
        // mappings on the dependency itself pins the channel for that configuration too. ForgeGradle invokes the
        // closure as a Groovy DSL closure (doCall, delegate set to the dependency), so the Action overload cannot be
        // used: it casts the dependency to a ClosureOwner it is not.
        Closure<Object> configureMappings = new Closure<Object>(project) {
            @SuppressWarnings("unused") // invoked by Groovy
            public Object doCall(Object dependency) {
                ((ExternalModuleDependency) dependency).attributes(attributes -> {
                    attributes.attribute(MAPPINGS_CHANNEL, "parchment");
                    attributes.attribute(MAPPINGS_VERSION, parchment + "-" + platform.minecraft());
                });
                return null;
            }
        };
        MavenizerInstance minecraftDependency = parchment == null
                ? minecraft.dependency(coordinates)
                : minecraft.dependency((Object) coordinates, configureMappings);
        deps.addProvider("implementation", minecraftDependency.asProvider());

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
