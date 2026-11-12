package lv.cebbys.modularity.gradle.setup;

import lv.cebbys.modularity.gradle.AssembleLayersTask;
import lv.cebbys.modularity.gradle.GenerateModMetadataTask;
import lv.cebbys.modularity.gradle.Layers;
import lv.cebbys.modularity.gradle.PlatformTable;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.plugins.BasePluginExtension;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * One platform project, configured in fixed stages. Every stage receives its own context record, so a stage can gain
 * an input without any other stage's signature changing; the contexts are package private and live here, with the
 * template that owns them.
 *
 * <p>The stage bodies of the loaders are scaffolded; the shared stages below and the Fabric setup are the first to be
 * filled in. The ordering in {@link #apply} is the contract.
 */
abstract class AbstractPlatformSetup {

    /** The directory of the loader layer roots, shared with the version parents and the docs. */
    static final String LAYER_BASE = "platforms";

    /** The nullness annotations every platform project compiles against, at compile time only (docs/07 7.3). */
    static final String ANNOTATIONS = "org.jspecify:jspecify:1.0.0";

    /** The stages, in order. The only entry point; never overridden. */
    final void apply(Project project, PlatformTable table, PlatformTable.Platform platform) {
        loaderPlugins(new LoaderPluginsContext(project, table, platform));
        javaSetup(new JavaSetupContext(project, table, platform));
        sources(new SourcesContext(project, table, platform, mainSourceSet(project)));
        dependencies(new DependenciesContext(project, table, platform, mainSourceSet(project)));
        tasks(new TasksContext(project, table, platform, mainSourceSet(project)));
        runs(new RunsContext(project, table, platform, mainSourceSet(project)));
        publication(new PublicationsContext(project, table, platform));
    }

    /** S1a: the loader's Gradle plugin(s). */
    protected abstract void loaderPlugins(LoaderPluginsContext ctx);

    /** S1b: the common Java setup: publication plugin, archive name, toolchain, sources jar, compile options. */
    protected void javaSetup(JavaSetupContext ctx) {
        Project project = ctx.project();
        PlatformTable.Platform platform = ctx.platform();
        project.getPluginManager().apply("maven-publish");

        project.getExtensions().getByType(BasePluginExtension.class).getArchivesName().set(archivesName(ctx));

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

        // Nullness: one annotation set for every platform project, compile time only (docs/07 7.3). The packages
        // carry @NullMarked, so a method with no annotation is non-null and an IDE stops asking for one.
        project.getDependencies().add("compileOnly", ANNOTATIONS);
    }

    /** S2: the layered sources and the generated mod metadata of the main source set. */
    protected void sources(SourcesContext ctx) {
        Project project = ctx.project();
        PlatformTable.Platform platform = ctx.platform();
        File tableFile = new File(project.getRootDir(), PlatformTable.FILE);

        // Layered sources: build/layered/main/{java,resources} + generated metadata.
        File layerBase = new File(project.getRootDir(), LAYER_BASE);
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
                    if (ctx.mod().icon() != null) {
                        t.getIconFile().set(project.getRootProject().getLayout().getProjectDirectory()
                                .file(ctx.mod().icon()));
                    }
                });

        SourceSet main = ctx.main();
        main.getJava().setSrcDirs(List.of(layered(project, assemble, "java")));
        main.getResources().setSrcDirs(List.of(layered(project, assemble, "resources")));
        main.getResources().srcDir(project.files(project.getLayout().getBuildDirectory().dir("generated/modularity"))
                .builtBy(metadata));

        ideLayerSources(project, platform, main);
    }

    /**
     * IDE-only source sets so IntelliJ indexes the layer folders themselves, not only the assembled copy under
     * {@code build/}. One source set per layer root, with the files a higher layer replaces or removes excluded, so
     * the IDE sees each path once. The sets are never compiled: their compile and resources tasks are disabled, and
     * nothing depends on them.
     */
    private static void ideLayerSources(Project project, PlatformTable.Platform platform, SourceSet main) {
        Path base = project.getRootDir().toPath().resolve(LAYER_BASE);
        List<Layers.Layer> layers;
        Map<String, String> winners = new HashMap<>();
        try {
            layers = Layers.resolve(base, platform.loader().layerRoots(), platform.minecraft(), "src/main");
            Layers.merge(layers).forEach((path, origin) -> winners.put(path, origin.layer()));
        } catch (Layers.LayerException e) {
            // The layer check runs again in assembleLayers, which fails the build; the IDE wiring is optional.
            project.getLogger().warn("{}: IDE layer sources skipped: {}", platform.id(), e.getMessage());
            return;
        }

        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        int index = 0;
        for (Layers.Layer layer : layers) {
            String name = "ideLayers" + index++;
            SourceSet ide = sourceSets.create(name);
            Path javaDir = layer.dir().resolve("java");
            Path resourcesDir = layer.dir().resolve("resources");
            List<String> javaExcludes = new ArrayList<>();
            List<String> resourceExcludes = new ArrayList<>();
            if (Files.isDirectory(javaDir)) {
                ide.getJava().setSrcDirs(List.of(javaDir.toFile()));
            }
            if (Files.isDirectory(resourcesDir)) {
                ide.getResources().setSrcDirs(List.of(resourcesDir.toFile()));
            }
            collectReplaced(layer, winners, javaExcludes, resourceExcludes);
            ide.getJava().exclude(javaExcludes.toArray(new String[0]));
            ide.getResources().exclude(resourceExcludes.toArray(new String[0]));

            // The IDE analyses these sources with the main class path; the set itself is never compiled.
            var configs = project.getConfigurations();
            configs.getByName(ide.getImplementationConfigurationName())
                    .extendsFrom(configs.getByName(main.getImplementationConfigurationName()));
            configs.getByName(ide.getCompileOnlyConfigurationName())
                    .extendsFrom(configs.getByName(main.getCompileOnlyConfigurationName()));
            configs.getByName(ide.getRuntimeOnlyConfigurationName())
                    .extendsFrom(configs.getByName(main.getRuntimeOnlyConfigurationName()));
            // Loom wires Minecraft and the remapped mods into the class path configurations only, not into
            // implementation, so copy the resolved class paths as well.
            configs.getByName(ide.getCompileClasspathConfigurationName())
                    .extendsFrom(configs.getByName(main.getCompileClasspathConfigurationName()));
            configs.getByName(ide.getRuntimeClasspathConfigurationName())
                    .extendsFrom(configs.getByName(main.getRuntimeClasspathConfigurationName()));
            // A source set sees neither its sibling source sets nor the assembled sources. Give it the compiled
            // main output so a symbol from another layer (e.g. platform.EntryPoints) resolves in the IDE.
            project.getDependencies().add(ide.getImplementationConfigurationName(), main.getOutput());
            String capitalized = Character.toUpperCase(name.charAt(0)) + name.substring(1);
            project.getTasks().matching(t -> t.getName().equals("compile" + capitalized + "Java")
                            || t.getName().equals("process" + capitalized + "Resources"))
                    .configureEach(t -> t.setEnabled(false));
        }
    }

    /** Adds every file of {@code layer} that a higher layer replaces or removes to the exclusion lists. */
    private static void collectReplaced(Layers.Layer layer, Map<String, String> winners,
                                        List<String> javaExcludes, List<String> resourceExcludes) {
        try (Stream<Path> walk = Files.walk(layer.dir())) {
            for (Path file : walk.filter(Files::isRegularFile).toList()) {
                String path = layer.dir().relativize(file).toString().replace('\\', '/');
                if (layer.label().equals(winners.get(path))) {
                    continue;
                }
                if (path.startsWith("java/")) {
                    javaExcludes.add(path.substring("java/".length()));
                } else if (path.startsWith("resources/")) {
                    resourceExcludes.add(path.substring("resources/".length()));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** S3: Minecraft and its mappings, the loader, the APIs, the nested modules, the separate mods. */
    protected abstract void dependencies(DependenciesContext ctx);

    /** S4: the loader-specific task wiring. Not every loader has any. */
    protected void tasks(TasksContext ctx) {
    }

    /** S5: the development and smoke runs. */
    protected abstract void runs(RunsContext ctx);

    /** S6: the {@code maven} publication of the platform jar. */
    protected void publication(PublicationsContext ctx) {
        ctx.project().getExtensions().getByType(PublishingExtension.class).getPublications()
                .create("maven", MavenPublication.class, pub -> {
                    pub.setArtifactId(archivesName(ctx));
                    pub.from(ctx.project().getComponents().getByName("java"));
                });
    }

    /** {@code <archive prefix>-<platform id>}: the name of the platform jar and of its publication. */
    private static String archivesName(PlatformView ctx) {
        return ctx.mod().archivePrefix() + "-" + ctx.platform().id();
    }

    private static Object layered(Project project, TaskProvider<AssembleLayersTask> assemble, String dir) {
        // Fixed paths (Loom reads source dirs while configuring, before task outputs may be queried).
        return project.files(project.getLayout().getBuildDirectory().dir("layered/main/" + dir)).builtBy(assemble);
    }

    /**
     * The repository Parchment is published to, filtered to it: it is a single artifact group, not a proxy for
     * everything else.
     */
    protected static void parchmentRepository(Project project) {
        if (project.getRepositories().findByName("Parchment") != null) {
            return;
        }
        project.getRepositories().maven(r -> {
            r.setName("Parchment");
            r.setUrl("https://maven.parchmentmc.org/");
            r.content(c -> c.includeGroup("org.parchmentmc.data"));
        });
    }

    /** What every stage context reads: the project it configures and the platform it builds. */
    interface PlatformView {
        Project project();

        PlatformTable table();

        PlatformTable.Platform platform();

        /** The one mod of the table, for the stages that generate its metadata. */
        default PlatformTable.Mod mod() {
            return table().mod();
        }

        default DependencyHandler dependencies() {
            return project().getDependencies();
        }

        default RepositoryHandler repositories() {
            return project().getRepositories();
        }

        default TaskContainer tasks() {
            return project().getTasks();
        }
    }

    /**
     * The main source set, looked up when the stage that needs it is built: it exists only once the loader's plugins
     * (which apply the Java plugin) have been applied by {@link #loaderPlugins}.
     */
    private static SourceSet mainSourceSet(Project project) {
        return project.getExtensions().getByType(SourceSetContainer.class).getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    }

    /** S1a: the loader's Gradle plugin(s) and nothing else. */
    record LoaderPluginsContext(Project project, PlatformTable table, PlatformTable.Platform platform)
            implements PlatformView {
    }

    /** S1b: the common Java setup (publication plugin, archive name, toolchain, compile options). */
    record JavaSetupContext(Project project, PlatformTable table, PlatformTable.Platform platform)
            implements PlatformView {
    }

    /** S2: the layered sources and the generated mod metadata of the main source set. */
    record SourcesContext(Project project, PlatformTable table, PlatformTable.Platform platform, SourceSet main)
            implements PlatformView {
    }

    /** S3: Minecraft, the loader, the APIs, the nested modules and the separate mods. */
    record DependenciesContext(Project project, PlatformTable table, PlatformTable.Platform platform, SourceSet main)
            implements PlatformView {
    }

    /** S4: the loader-specific task wiring (Forge's jar-in-jar, Quilt's launch properties). */
    record TasksContext(Project project, PlatformTable table, PlatformTable.Platform platform, SourceSet main)
            implements PlatformView {
    }

    /** S5: the development and smoke runs. */
    record RunsContext(Project project, PlatformTable table, PlatformTable.Platform platform, SourceSet main)
            implements PlatformView {
    }

    /** S6: the {@code maven} publication of the platform jar. */
    record PublicationsContext(Project project, PlatformTable table, PlatformTable.Platform platform)
            implements PlatformView {
    }
}
