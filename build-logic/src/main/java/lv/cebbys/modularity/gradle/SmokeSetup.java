package lv.cebbys.modularity.gradle;

import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Smoke tests of one platform: the layered smoke mod ({@code tools/smoke}) as source set {@code smoke}, Loom runs
 * {@code runSmokeServer} / {@code runSmokeClient} in {@code build/smoke/run}, and the checks {@code smokeServer},
 * {@code smokeClient} and {@code smoke} (server, plus client where the table asks for it).
 */
final class SmokeSetup {

    static final String SMOKE_ROOT = "tools/smoke";
    static final String RUN_DIR = "build/smoke/run";

    private SmokeSetup() {
    }

    static void apply(Project project, PlatformTable.Platform platform, LoomGradleExtensionAPI loom, SourceSet main) {
        TaskProvider<AssembleLayersTask> assemble = project.getTasks().register("assembleSmokeLayers",
                AssembleLayersTask.class, t -> {
                    t.setGroup("modularity");
                    t.setDescription("Assembles the smoke mod layers of " + platform.minecraft());
                    t.getBaseDir().set(new File(project.getRootDir(), SMOKE_ROOT));
                    t.getMinecraft().set(platform.minecraft().toString());
                    t.getSourceSet().set("src/main");
                    t.getOutputDir().set(project.getLayout().getBuildDirectory().dir("layered/smoke"));
                    t.getOriginsFile().set(project.getLayout().getBuildDirectory().file("layered/smoke-origins.txt"));
                });

        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        SourceSet smoke = sourceSets.create("smoke");
        smoke.getJava().setSrcDirs(List.of(dir(project, assemble, "java")));
        smoke.getResources().setSrcDirs(List.of(dir(project, assemble, "resources")));
        FileCollection mainClasses = main.getOutput();
        smoke.setCompileClasspath(mainClasses.plus(main.getCompileClasspath()));
        smoke.setRuntimeClasspath(smoke.getOutput().plus(main.getRuntimeClasspath()));

        // Each source set is its own mod in development (Fabric's classpath groups).
        loom.getMods().register("modularity", m -> m.sourceSet(main));
        loom.getMods().register("modularity_smoke", m -> m.sourceSet(smoke));

        String userDir = new File(project.getProjectDir(), RUN_DIR).getAbsolutePath();
        loom.getRuns().register("smokeServer", r -> {
            r.server();
            r.getDisplayName().set("Smoke server " + platform.name());
            r.getSourceSet().set(smoke.getName());
            r.getRunDirectory().set(project.getLayout().getProjectDirectory().dir(RUN_DIR));
            r.getGenerateRunConfig().set(false);
        });
        loom.getRuns().register("smokeClient", r -> {
            r.client();
            r.getDisplayName().set("Smoke client " + platform.name());
            r.getSourceSet().set(smoke.getName());
            r.getRunDirectory().set(project.getLayout().getProjectDirectory().dir(RUN_DIR));
            r.getGenerateRunConfig().set(false);
            r.getProgramArguments().addAll("--quickPlaySingleplayer", "smoke", "--width", "854", "--height", "480");
        });

        // Own port per platform as well, in case a server of another platform is still shutting down.
        int port = 25600 + Math.floorMod(platform.minecraft().hashCode(), 300);
        var prepare = project.getTasks().register("prepareSmokeServer", t -> {
            t.setGroup("verification");
            t.setDescription("Fresh run dir for the smoke server: eula, flat world settings");
            t.doLast(x -> prepareServer(Path.of(userDir), port));
        });
        var runs = project.getGradle().getSharedServices().registerIfAbsent(SmokeRunService.NAME, SmokeRunService.class,
                spec -> spec.getMaxParallelUsages().set(1));
        project.getTasks().named("runSmokeServer", t -> {
            t.dependsOn(prepare);
            t.usesService(runs);
        });

        String mc = platform.minecraft().toString();
        var server = project.getTasks().register("smokeServer", SmokeCheckTask.class, t -> {
            t.setGroup("verification");
            t.setDescription("Dedicated server smoke test of " + mc);
            t.dependsOn("runSmokeServer");
            t.getLog().set(new File(userDir, "logs/latest.log"));
            t.getMinecraft().set(mc);
            t.getSide().set(SmokeCheck.Side.SERVER);
            t.getSummary().set(project.getLayout().getBuildDirectory().file("smoke/server.txt"));
            t.getOutputs().upToDateWhen(x -> false);  // checks the run that just happened
        });

        // The client opens the world the server run wrote.
        var copyWorld = project.getTasks().register("copySmokeWorld", Sync.class, t -> {
            t.dependsOn(server);
            t.from(new File(userDir, "smoke"));
            t.into(new File(userDir, "saves/smoke"));
        });
        project.getTasks().named("runSmokeClient", t -> {
            t.usesService(runs);
            t.dependsOn(copyWorld);
            t.doFirst(x -> writeOptions(Path.of(userDir)));
        });
        var client = project.getTasks().register("smokeClient", SmokeCheckTask.class, t -> {
            t.setGroup("verification");
            t.setDescription("Client smoke test of " + mc);
            t.dependsOn("runSmokeClient");
            t.getLog().set(new File(userDir, "logs/latest.log"));
            t.getMinecraft().set(mc);
            t.getSide().set(SmokeCheck.Side.CLIENT);
            t.getSummary().set(project.getLayout().getBuildDirectory().file("smoke/client.txt"));
            t.getOutputs().upToDateWhen(x -> false);
        });

        project.getTasks().register("smoke", t -> {
            t.setGroup("verification");
            t.setDescription("Smoke tests of " + mc + (platform.smokeClient() ? " (server and client)" : " (server)"));
            t.dependsOn(server);
            if (platform.smokeClient()) {
                t.dependsOn(client);
            }
        });
    }

    private static void prepareServer(Path dir, int port) {
        try {
            for (String sub : List.of("smoke", "saves", "logs")) {
                delete(dir.resolve(sub));
            }
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("eula.txt"), "eula=true\n");
            Files.writeString(dir.resolve("server.properties"), String.join("\n",
                    "level-name=smoke", "level-type=minecraft\\:flat", "online-mode=false", "spawn-protection=0",
                    // Explicit flat preset: with none, vanilla logs "No key layers" and falls back to the default.
                    "generator-settings={\"layers\":[{\"block\":\"minecraft:bedrock\",\"height\":1},"
                            + "{\"block\":\"minecraft:dirt\",\"height\":2},{\"block\":\"minecraft:grass_block\",\"height\":1}],"
                            + "\"biome\":\"minecraft:plains\"}",
                    "generate-structures=false", "spawn-monsters=false", "spawn-animals=false", "server-port=" + port,
                    "") );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeOptions(Path dir) {
        try {
            Files.writeString(dir.resolve("options.txt"), String.join("\n",
                    "onboardAccessibility:false", "pauseOnLostFocus:false", "renderDistance:4",
                    "simulationDistance:5", "skipMultiplayerWarning:true", "joinedFirstServer:true",
                    "tutorialStep:none", "soundCategory_master:0.0", ""));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void delete(Path p) throws IOException {
        if (Files.exists(p)) {
            try (Stream<Path> walk = Files.walk(p)) {
                for (Path f : walk.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(f);
                }
            }
        }
    }

    private static Object dir(Project project, TaskProvider<AssembleLayersTask> assemble, String sub) {
        return project.files(project.getLayout().getBuildDirectory().dir("layered/smoke/" + sub)).builtBy(assemble);
    }
}
