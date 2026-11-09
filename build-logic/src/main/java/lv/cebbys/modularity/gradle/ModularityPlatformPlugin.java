package lv.cebbys.modularity.gradle;

import lv.cebbys.modularity.gradle.setup.PlatformSetup;
import lv.cebbys.modularity.gradle.smoke.SmokeSetup;
import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.BasePluginExtension;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * One platform project ({@code :platforms:<name>:<name>-<loader>}), configured from {@code gradle/platforms.toml}:
 * the loader's Gradle plugin, Java target, layered sources from the loader's layer roots under {@code platforms/},
 * generated mod metadata, dependencies, publication. Fabric and Quilt use Loom (remap with Mojang mappings for
 * obfuscated Minecraft, plain from 26.1).
 */
public class ModularityPlatformPlugin implements Plugin<Project> {

    public static final String LAYER_BASE = "platforms";

    @Override
    public void apply(Project project) {
        File rootDir = project.getRootDir();
        File tableFile = new File(rootDir, PlatformTable.FILE);
        PlatformTable table = PlatformTable.read(tableFile.toPath());
        PlatformTable.Platform platform = table.platformByProjectName(project.getName())
                .orElseThrow(() -> new GradleException(project.getPath() + " is not in " + PlatformTable.FILE));
        PlatformTable.Mod mod = table.mod();
        if (platform.loader() == Loader.FABRIC) {
            // Fabric is configured by its own setup (gradle.setup); the other loaders keep the legacy path until their
            // setups are filled in.
            new PlatformSetup().apply(project, table, platform);
            return;
        }
        switch (platform.loader()) {
            case QUILT -> project.getPluginManager()
                    .apply(platform.remap() ? "net.fabricmc.fabric-loom-remap" : "net.fabricmc.fabric-loom");
            case NEOFORGE -> {
                project.getPluginManager().apply("java-library");
                project.getPluginManager().apply("net.neoforged.moddev");
            }
            case FORGE -> {
                project.getPluginManager().apply("java-library");
                project.getPluginManager().apply("net.minecraftforge.gradle");
            }
        }
        project.getPluginManager().apply("maven-publish");

        String archivesName = mod.archivePrefix() + "-" + platform.id();
        project.getExtensions().getByType(BasePluginExtension.class).getArchivesName().set(archivesName);

        JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
        java.getToolchain().getLanguageVersion().set(JavaLanguageVersion.of(platform.java()));
        java.withSourcesJar();
        project.getTasks().withType(JavaCompile.class).configureEach(t -> {
            t.getOptions().getRelease().set(platform.java());
            t.getOptions().setEncoding("UTF-8");
        });

        // The Java plugin calls it compileJava; the version parents and the docs use compile everywhere.
        project.getTasks().register("compile", t -> {
            t.setGroup("build");
            t.setDescription("Compiles the Java sources of " + platform.id());
            t.dependsOn("compileJava");
        });

        // Layered sources: build/layered/main/{java,resources} + generated metadata.
        File layerBase = new File(rootDir, LAYER_BASE);
        TaskProvider<AssembleLayersTask> assemble = project.getTasks().register("assembleLayers", AssembleLayersTask.class, t -> {
            t.setGroup("modularity");
            t.setDescription("Assembles the layers of " + platform.id() + ": " + platform.loader().layerRoots());
            t.getBaseDir().set(layerBase);
            t.getRoots().set(platform.loader().layerRoots());
            t.getMinecraft().set(platform.minecraft().toString());
            t.getSourceSet().set("src/main");
            t.getOutputDir().set(project.getLayout().getBuildDirectory().dir("layered/main"));
            t.getOriginsFile().set(project.getLayout().getBuildDirectory().file("layered/origins.txt"));
        });
        TaskProvider<GenerateModMetadataTask> metadata = project.getTasks().register("generateModMetadata",
                GenerateModMetadataTask.class, t -> {
                    t.setGroup("modularity");
                    t.getTableFile().set(tableFile);
                    t.getPlatform().set(platform.id());
                    t.getModVersion().set(project.provider(() -> project.getVersion().toString()));
                    t.dependsOn(assemble);
                    t.getJavaDir().set(project.getLayout().getBuildDirectory().dir("layered/main/java"));
                    t.getOutputDir().set(project.getLayout().getBuildDirectory().dir("generated/modularity"));
                    if (table.mod().icon() != null) {
                        t.getIconFile().set(project.getRootProject().getLayout().getProjectDirectory()
                                .file(table.mod().icon()));
                    }
                });

        SourceSet main = project.getExtensions().getByType(SourceSetContainer.class).getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        main.getJava().setSrcDirs(List.of(layered(project, assemble, "java")));
        main.getResources().setSrcDirs(List.of(layered(project, assemble, "resources")));
        main.getResources().srcDir(project.files(project.getLayout().getBuildDirectory().dir("generated/modularity"))
                .builtBy(metadata));

        switch (platform.loader()) {
            case QUILT -> fabric(project, table, platform, main);
            case NEOFORGE -> NeoForgeSetup.apply(project, table, platform, main);
            case FORGE -> ForgeSetup.apply(project, table, platform, main);
            default -> throw new GradleException("unexpected loader " + platform.loader());
        }

        project.getExtensions().getByType(PublishingExtension.class).getPublications()
                .create("maven", MavenPublication.class, pub -> {
                    pub.setArtifactId(archivesName);
                    pub.from(project.getComponents().getByName("java"));
                });
    }

    /** Loom (Fabric, Quilt): Minecraft with Mojang mappings where obfuscated, loader, Fabric API, mods, smoke runs. */
    private static void fabric(Project project, PlatformTable table, PlatformTable.Platform platform, SourceSet main) {
        PlatformTable.Mod mod = table.mod();
        LoomGradleExtensionAPI loom = project.getExtensions().getByType(LoomGradleExtensionAPI.class);
        DependencyHandler deps = project.getDependencies();
        deps.add("minecraft", "com.mojang:minecraft:" + platform.minecraft());
        if (platform.remap()) {
            deps.add("mappings", mappings(project, loom, platform));
        }
        String modConfig = platform.remap() ? "modImplementation" : "implementation";
        if (platform.loader() == Loader.QUILT) {
            QuiltSetup.addLoader(project, modConfig, platform.loaderVersion());
            QuiltSetup.mirrorLaunchProperties(project);
            // Compile exactly what the Fabric jar compiles (the guest loader uses Fabric Loader classes, unused on
            // Quilt); at runtime only Quilt Loader is present.
            deps.add("compileOnly", "net.fabricmc:fabric-loader:" + table.fabricLoader());
            // Fabric API depends on Fabric Loader (followed for unremapped 26.x dependencies); on Quilt only Quilt
            // Loader may be on the run class path (both ship net.fabricmc.loader.impl classes).
            project.getConfigurations().matching(c -> c.getName().endsWith("RuntimeClasspath")
                    || c.getName().equals("runtimeClasspath")).configureEach(c ->
                    c.exclude(Map.of("group", "net.fabricmc", "module", "fabric-loader")));
        } else {
            deps.add(modConfig, "net.fabricmc:fabric-loader:" + platform.loaderVersion());
        }
        deps.add(modConfig, "net.fabricmc.fabric-api:fabric-api:" + platform.fabricApi());
        for (String path : mod.nested()) {
            deps.add("api", deps.project(Map.of("path", path)));
            deps.add("include", deps.project(Map.of("path", path)));
        }
        // Separate mods, not nested: declared in fabric.mod.json "depends". Same build and already named,
        // so Loom must not remap (and cache) them.
        for (String path : mod.mods()) {
            deps.add("implementation", deps.project(Map.of("path", path, "configuration", "namedElements")));
        }
        if (platform.modmenu() != null) {
            project.getRepositories().maven(r -> {
                r.setName("TerraformersMC");
                r.setUrl("https://maven.terraformersmc.com/releases/");
                r.content(c -> c.includeGroup("com.terraformersmc"));
            });
            // Development only (runClient): lists the loaded mods.
            deps.add(platform.remap() ? "modLocalRuntime" : "localRuntime", "com.terraformersmc:modmenu:" + platform.modmenu());
        }

        // Remapping is memory hungry: a few at a time across all platforms.
        var remaps = project.getGradle().getSharedServices().registerIfAbsent(RemapService.NAME, RemapService.class,
                spec -> spec.getMaxParallelUsages().set(RemapService.MAX_PARALLEL));
        project.getTasks().matching(t -> t.getName().startsWith("remap")).configureEach(t -> t.usesService(remaps));

        SmokeSetup.apply(project, platform, main, SmokeSetup.loom(loom));
        if (platform.loader() == Loader.QUILT) {
            QuiltSetup.mainClasses(loom, project);
        }

    }

    /**
     * The mappings of obfuscated Minecraft: official Mojang names, layered with Parchment (parameter names and
     * javadoc) where the table has a Parchment version for it. Parchment is compile-time metadata only: class and
     * member names, the remapped jar and the runtime are the same either way.
     */
    private static Object mappings(Project project, LoomGradleExtensionAPI loom, PlatformTable.Platform platform) {
        if (platform.parchment() == null) {
            return loom.officialMojangMappings();
        }
        parchmentRepository(project);
        return loom.layered(builder -> {
            builder.officialMojangMappings();
            builder.parchment(platform.parchmentArtifact());
        });
    }

    /**
     * The repository Parchment is published to, filtered to it: it is a single artifact group, not a proxy for
     * everything else. Shared with the NeoForge and Forge setups.
     */
    public static void parchmentRepository(Project project) {
        if (project.getRepositories().findByName("Parchment") != null) {
            return;
        }
        project.getRepositories().maven(r -> {
            r.setName("Parchment");
            r.setUrl("https://maven.parchmentmc.org/");
            r.content(c -> c.includeGroup("org.parchmentmc.data"));
        });
    }

    /** The jar task that produces the mod jar: {@code remapJar} for obfuscated versions, else {@code jar}. */
    public static String modJarTask(PlatformTable.Platform platform) {
        return platform.remap() ? "remapJar" : "jar";
    }

    private static Object layered(Project project, TaskProvider<AssembleLayersTask> assemble, String dir) {
        // Fixed paths (Loom reads source dirs while configuring, before task outputs may be queried).
        return project.files(project.getLayout().getBuildDirectory().dir("layered/main/" + dir)).builtBy(assemble);
    }
}
