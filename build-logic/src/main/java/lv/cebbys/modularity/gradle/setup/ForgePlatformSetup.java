package lv.cebbys.modularity.gradle.setup;

import groovy.lang.Closure;
import lv.cebbys.modularity.gradle.JarJarMetadataTask;
import lv.cebbys.modularity.gradle.Loader;
import lv.cebbys.modularity.gradle.ModMetadata;
import lv.cebbys.modularity.gradle.PlatformTable;
import lv.cebbys.modularity.gradle.ServiceFileTask;
import lv.cebbys.modularity.gradle.UniversalJar;
import lv.cebbys.modularity.gradle.smoke.SmokeSetup;
import net.minecraftforge.gradle.ForgeGradleExtension;
import net.minecraftforge.gradle.MavenizerInstance;
import net.minecraftforge.gradle.MinecraftExtensionForProject;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;

import java.util.List;
import java.util.Map;

/**
 * Forge platform with ForgeGradle 7: official Mojang mappings (layered with Parchment where the table has a version
 * for the Minecraft version), Forge from the Mavenizer repository, nested modules in Forge's jar-in-jar format
 * ({@code META-INF/jarjar}, which ForgeGradle 7 does not write), the Forge builds of the separate mods, smoke runs.
 */
class ForgePlatformSetup extends AbstractForgeFamilyPlatformSetup {

    /** The attributes ForgeGradle's Mavenizer puts on the variants of a Forge dependency (ForgeAttributes). */
    private static final Attribute<String> MAPPINGS_CHANNEL =
            Attribute.of("net.minecraftforge.mappings.channel", String.class);
    private static final Attribute<String> MAPPINGS_VERSION =
            Attribute.of("net.minecraftforge.mappings.version", String.class);

    /** The configuration the nested modules are declared in, read back by {@link #tasks}. */
    private static final String NESTED_CONFIGURATION = "modularityNested";

    @Override
    protected String loaderPluginId() {
        return "net.minecraftforge.gradle";
    }

    /** Forge's development mod discovery reads each class path folder as its own mod: classes and resources together. */
    @Override
    protected void prepareSourceSet(SourceSet sourceSet) {
        sourceSet.getOutput().setResourcesDir(sourceSet.getJava().getDestinationDirectory().get().getAsFile());
    }

    @Override
    protected void loaderDependencies(DependenciesContext ctx) {
        Project project = ctx.project();
        PlatformTable.Platform platform = ctx.platform();
        MinecraftExtensionForProject minecraft = project.getExtensions().getByType(MinecraftExtensionForProject.class);
        ForgeGradleExtension fg = project.getExtensions().getByType(ForgeGradleExtension.class);

        String parchment = platform.parchment();
        if (parchment == null) {
            minecraft.mappings("official", platform.minecraft().toString());
        } else {
            parchmentRepository(project);
            // ForgeGradle sanitizes the version to <timestamp>-<minecraft> (ParchmentVersion.toFriendly()).
            minecraft.mappings("parchment", parchment + "-" + platform.minecraft());
        }
        minecraft.mavenizer(project.getRepositories());
        project.getRepositories().maven(fg.getForgeMaven());
        project.getRepositories().maven(fg.getMinecraftLibsMaven());

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
        ctx.dependencies().addProvider("implementation", minecraftDependency.asProvider());
    }

    /** Forge's jar-in-jar: the modules are declared in their own configuration for the {@code jar} task to pick up. */
    @Override
    protected void packageNestedModules(DependenciesContext ctx) {
        Configuration nested = ctx.project().getConfigurations().create(NESTED_CONFIGURATION, c -> {
            c.setCanBeConsumed(false);
            c.setTransitive(false);
        });
        for (String path : ctx.mod().nested()) {
            ctx.dependencies().add(nested.getName(), ctx.dependencies().project(Map.of("path", path)));
        }
    }

    /** Forge's jar-in-jar: {@code META-INF/jarjar/metadata.json} plus the nested jars themselves. */
    @Override
    protected void tasks(TasksContext ctx) {
        Project project = ctx.project();
        List<String> nestedIds = ctx.mod().nested();
        TaskProvider<JarJarMetadataTask> jarJarMetadata = project.getTasks().register("jarJarMetadata",
                JarJarMetadataTask.class, t -> {
                    t.setGroup("modularity");
                    t.setDescription("Writes META-INF/jarjar/metadata.json for the nested modules of " + ctx.platform().id());
                    t.getEntries().set(project.provider(() -> nestedIds.stream().map(path -> {
                        Project nested = project.project(path);
                        return nested.getGroup() + ":" + nested.getName() + ":" + nested.getVersion();
                    }).toList()));
                    t.getOutputDir().set(project.getLayout().getBuildDirectory().dir("generated/jarjar"));
                });
        Configuration nested = project.getConfigurations().getByName(NESTED_CONFIGURATION);
        String mixinConfig = ModMetadata.mixinConfigName(ctx.mod(), Loader.FORGE);
        // FML offers a mods/ jar to ModLauncher only when the jar provides a locator service, and Mixin then reads
        // the manifest attribute below. The file goes into the jar alone: on the development class path FML reads
        // that entry as a transformer artifact and stops reading it as a mod (docs/07 7.8).
        TaskProvider<ServiceFileTask> locatorService = project.getTasks().register("forgeLocatorService",
                ServiceFileTask.class, t -> {
                    t.setGroup("modularity");
                    t.setDescription("Writes the Forge locator service file of the jar, never of the run class path");
                    t.getService().set("net.minecraftforge.forgespi.locating.IDependencyLocator");
                    t.getProvider().set(UniversalJar.ROOT.replace('/', '.')
                            + ".forge.locating.ModularityForgeLocator");
                    t.getOutputDir().set(project.getLayout().getBuildDirectory().dir("generated/forge-locator"));
                });
        project.getTasks().named("jar", Jar.class, jar -> {
            // Forge's mods.toml has no [[mixins]] block. Mixin reads this attribute from the manifest of every jar
            // ModLauncher scans (docs/07 7.8), so the Forge config reaches Mixin the way the Mixin wiki gives.
            jar.getManifest().attributes(Map.of(ModMetadata.MANIFEST_MIXIN_CONFIGS, mixinConfig));
            // The generated metadata of the development output holds the same attribute, so a development run applies
            // the config (GenerateModMetadataTask). The jar writes its own manifest, so that copy must not reach it.
            jar.exclude("META-INF/MANIFEST.MF");
            jar.from(jarJarMetadata);
            jar.from(locatorService);
            jar.from(nested, spec -> {
                spec.into("META-INF/jarjar");
                spec.rename(name -> nestedJarName(project, nestedIds, name));
            });
        });
    }

    @Override
    protected SmokeSetup.RunTasks registerSmokeRuns(RunsContext ctx, SourceSet smoke, String guestMods) {
        Project project = ctx.project();
        MinecraftExtensionForProject minecraft = project.getExtensions().getByType(MinecraftExtensionForProject.class);
        // ForgeGradle fills a run (main class, arguments) from Forge's run configuration of the same name and registers
        // its task per source set: run<Name> for main, run<SourceSet><Name> (runSmokeServer) for smoke, with that
        // source set's class path.
        for (String side : List.of("server", "client")) {
            minecraft.getRuns().register(side, r -> {
                r.getWorkingDir().set(project.getLayout().getProjectDirectory().dir(SmokeSetup.RUN_DIR));
                r.args(side.equals("server") ? List.of("--nogui") : SmokeSetup.CLIENT_ARGS);
                r.getMods().register(ctx.mod().id(), m -> m.source(ctx.main()));
                r.getMods().register("modularity_smoke", m -> m.source(smoke));
                if (guestMods != null) {
                    r.systemProperty("modularity.guestMods", guestMods);
                }
            });
        }
        return SmokeSetup.RunTasks.SMOKE;
    }

    /** {@code group.artifact-version.jar}, as ModDevGradle names nested jars. */
    private static String nestedJarName(Project project, List<String> paths, String fileName) {
        for (String path : paths) {
            Project nested = project.project(path);
            if (fileName.startsWith(nested.getName() + "-")) {
                return nested.getGroup() + "." + nested.getName() + "-" + nested.getVersion() + ".jar";
            }
        }
        return fileName;
    }
}
