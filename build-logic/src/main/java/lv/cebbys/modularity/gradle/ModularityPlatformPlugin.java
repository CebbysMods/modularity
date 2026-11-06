package lv.cebbys.modularity.gradle;

import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.Directory;
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
 * One Fabric platform project ({@code :platforms:fabric-<name>}), configured from {@code gradle/platforms.toml}:
 * Loom (remap with Mojang mappings for obfuscated Minecraft, plain from 26.1), Java target, layered sources from
 * {@code platforms/fabric}, generated {@code fabric.mod.json} and mixin config, dependencies, publication.
 */
public class ModularityPlatformPlugin implements Plugin<Project> {

    public static final String LAYER_ROOT = "platforms/fabric";

    @Override
    public void apply(Project project) {
        File rootDir = project.getRootDir();
        File tableFile = new File(rootDir, PlatformTable.FILE);
        PlatformTable table = PlatformTable.read(tableFile.toPath());
        String name = project.getName().replaceFirst("^fabric-", "");
        PlatformTable.Platform platform = table.platform(name)
                .orElseThrow(() -> new GradleException(project.getPath() + " is not in " + PlatformTable.FILE));
        PlatformTable.Mod mod = table.mod();

        project.getPluginManager().apply(platform.remap() ? "net.fabricmc.fabric-loom-remap" : "net.fabricmc.fabric-loom");
        project.getPluginManager().apply("maven-publish");

        String archivesName = mod.archivePrefix() + "-fabric-" + name;
        project.getExtensions().getByType(BasePluginExtension.class).getArchivesName().set(archivesName);

        JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
        java.getToolchain().getLanguageVersion().set(JavaLanguageVersion.of(platform.java()));
        java.withSourcesJar();
        project.getTasks().withType(JavaCompile.class).configureEach(t -> {
            t.getOptions().getRelease().set(platform.java());
            t.getOptions().setEncoding("UTF-8");
        });

        // Layered sources: build/layered/main/{java,resources} + generated metadata.
        File layerRoot = new File(rootDir, LAYER_ROOT);
        TaskProvider<AssembleLayersTask> assemble = project.getTasks().register("assembleLayers", AssembleLayersTask.class, t -> {
            t.setGroup("modularity");
            t.setDescription("Assembles common, changes and version layers of " + platform.minecraft());
            t.getBaseDir().set(layerRoot);
            t.getMinecraft().set(platform.minecraft().toString());
            t.getSourceSet().set("src/main");
            t.getOutputDir().set(project.getLayout().getBuildDirectory().dir("layered/main"));
            t.getOriginsFile().set(project.getLayout().getBuildDirectory().file("layered/origins.txt"));
        });
        TaskProvider<GenerateModMetadataTask> metadata = project.getTasks().register("generateModMetadata",
                GenerateModMetadataTask.class, t -> {
                    t.setGroup("modularity");
                    t.getTableFile().set(tableFile);
                    t.getPlatform().set(name);
                    t.getModVersion().set(project.provider(() -> project.getVersion().toString()));
                    t.getJavaDir().set(assemble.flatMap(a -> a.getOutputDir().dir("java")));
                    t.getOutputDir().set(project.getLayout().getBuildDirectory().dir("generated/modularity"));
                });

        SourceSet main = project.getExtensions().getByType(SourceSetContainer.class).getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        main.getJava().setSrcDirs(List.of(layered(project, assemble, "java")));
        main.getResources().setSrcDirs(List.of(layered(project, assemble, "resources")));
        main.getResources().srcDir(metadata.flatMap(GenerateModMetadataTask::getOutputDir));

        // Dependencies.
        LoomGradleExtensionAPI loom = project.getExtensions().getByType(LoomGradleExtensionAPI.class);
        DependencyHandler deps = project.getDependencies();
        deps.add("minecraft", "com.mojang:minecraft:" + platform.minecraft());
        if (platform.remap()) {
            deps.add("mappings", loom.officialMojangMappings());
        }
        String modConfig = platform.remap() ? "modImplementation" : "implementation";
        deps.add(modConfig, "net.fabricmc:fabric-loader:" + platform.loader());
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

        project.getExtensions().getByType(PublishingExtension.class).getPublications()
                .create("maven", MavenPublication.class, pub -> {
                    pub.setArtifactId(archivesName);
                    pub.from(project.getComponents().getByName("java"));
                });
    }

    /** The jar task that produces the mod jar: {@code remapJar} for obfuscated versions, else {@code jar}. */
    public static String modJarTask(PlatformTable.Platform platform) {
        return platform.remap() ? "remapJar" : "jar";
    }

    private static Object layered(Project project, TaskProvider<AssembleLayersTask> assemble, String dir) {
        return project.files(assemble.flatMap(a -> a.getOutputDir().map((Directory d) -> d.dir(dir))))
                .builtBy(assemble);
    }
}
