package lv.cebbys.modularity.gradle.setup;

import lv.cebbys.modularity.gradle.Loader;
import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.JavaExec;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Quilt platform: everything Fabric has, plus Quilt Loader with the libraries of its installer, the mirrored launch
 * properties and the Knot main classes, and without Fabric Loader on the runtime class path.
 *
 * <p>Loom reads the loader's libraries from {@code fabric-installer.json}; Quilt Loader ships
 * {@code quilt_installer.json} instead, so its libraries (Mixin, ASM, quilt-json5, quilt-config, MixinExtras for
 * development) are read from there and added here: the exact list of that loader version, nothing guessed.
 */
class QuiltPlatformSetup extends AbstractLoomPlatformSetup {

    private static final String MAVEN = "https://maven.quiltmc.org/repository/release/";
    private static final Pattern LIBRARY = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"url\"\\s*:\\s*\"([^\"]+)\"");
    private static final List<String> SECTIONS = List.of("common", "development");

    @Override
    protected void loaderDependency(DependenciesContext ctx, String modConfig) {
        assert ctx.platform().loader() == Loader.QUILT;
        Project project = ctx.project();
        project.getRepositories().maven(r -> {
            r.setName("Quilt");
            r.setUrl(MAVEN);
            r.content(c -> c.includeGroup("org.quiltmc"));
        });
        String loader = "org.quiltmc:quilt-loader:" + ctx.platform().loaderVersion();
        ctx.dependencies().add(modConfig, loader);

        // Added when "implementation" is first resolved, not while configuring (the root project applies this plugin,
        // and resolving another project's configuration then is not allowed).
        project.getConfigurations().getByName("implementation").withDependencies(set -> {
            Configuration probe = project.getConfigurations().detachedConfiguration(
                    project.getDependencies().create(loader));
            probe.setTransitive(false);
            for (String library : libraries(probe.getSingleFile())) {
                set.add(project.getDependencies().create(library));
            }
        });

        // The loom layer uses the Fabric Loader API (LoaderInfo, guest scan), which Quilt Loader provides; at
        // runtime only Quilt Loader is present.
        ctx.dependencies().add("compileOnly", "net.fabricmc:fabric-loader:" + ctx.table().fabricLoader());
    }

    @Override
    protected void runtimeConfigurations(DependenciesContext ctx) {
        // Fabric API depends on Fabric Loader (followed for unremapped 26.x dependencies); on Quilt only Quilt Loader
        // may be on the run class path (both ship net.fabricmc.loader.impl classes).
        ctx.project().getConfigurations().matching(c -> c.getName().endsWith("RuntimeClasspath")
                || c.getName().equals("runtimeClasspath")).configureEach(c ->
                c.exclude(Map.of("group", "net.fabricmc", "module", "fabric-loader")));
    }

    @Override
    protected void tasks(TasksContext ctx) {
        // Loom writes the development settings as fabric.* properties into launch.cfg; Quilt Loader reads the same
        // settings as loader.*. Without them Quilt finds none of the development mods. Just before each game run:
        // the launch.cfg path is in the run's JVM arguments (-Dfabric.dli.config=...).
        ctx.project().getTasks().withType(JavaExec.class).configureEach(t -> t.doFirst(_ -> {
            for (String arg : t.getAllJvmArgs()) {
                if (arg.startsWith("-Dfabric.dli.config=")) {
                    Path cfg = Path.of(arg.substring("-Dfabric.dli.config=".length()));
                    if (Files.isRegularFile(cfg)) {
                        mirror(cfg);
                    }
                }
            }
        }));
    }

    @SuppressWarnings("deprecation") // getEnvironment has no replacement in Loom 1.19
    @Override
    protected void runs(RunsContext ctx) {
        super.runs(ctx);
        // Quilt's game entry classes (quilt_installer.json "mainClass"); Loom would pick Fabric Loader's.
        LoomGradleExtensionAPI loom = ctx.project().getExtensions().getByType(LoomGradleExtensionAPI.class);
        loom.getRuns().configureEach(r -> r.getMainClass().set(ctx.project().provider(
                () -> "client".equals(r.getEnvironment())
                        ? "org.quiltmc.loader.impl.launch.knot.KnotClient"
                        : "org.quiltmc.loader.impl.launch.knot.KnotServer")));
    }

    private static void mirror(Path cfg) {
        try {
            List<String> out = new ArrayList<>();
            for (String line : Files.readAllLines(cfg)) {
                String key = line.strip();
                if (key.startsWith("loader.")) {
                    continue;  // written by an earlier run; mirrored again below
                }
                out.add(line);
                for (String setting : MIRRORED) {
                    if (key.startsWith("fabric." + setting + "=")) {
                        out.add(line.replace("fabric." + setting + "=", "loader." + setting + "="));
                    }
                }
            }
            Files.write(cfg, out);
        } catch (IOException e) {
            throw new GradleException("Cannot update " + cfg, e);
        }
    }

    private static final List<String> MIRRORED = List.of("development", "remapClasspathFile", "defaultMixinRemapType",
            "classPathGroups");

    /** {@code group:name:version} of the {@code common} and {@code development} libraries of the installer json. */
    private static List<String> libraries(File loaderJar) {
        try (ZipFile zip = new ZipFile(loaderJar)) {
            ZipEntry entry = zip.getEntry("quilt_installer.json");
            if (entry == null) {
                throw new GradleException(loaderJar + " has no quilt_installer.json");
            }
            String json;
            try (InputStream in = zip.getInputStream(entry)) {
                json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            return parse(json);
        } catch (IOException e) {
            throw new GradleException("Cannot read " + loaderJar, e);
        }
    }

    private static List<String> parse(String json) {
        List<String> out = new ArrayList<>();
        for (String section : SECTIONS) {
            int start = json.indexOf("\"" + section + "\"");
            if (start < 0) {
                continue;
            }
            int open = json.indexOf('[', start);
            int close = json.indexOf(']', open);
            Matcher m = LIBRARY.matcher(json.substring(open, close));
            while (m.find()) {
                out.add(m.group(1));
            }
        }
        return out;
    }
}
