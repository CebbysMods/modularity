package lv.cebbys.modularity.gradle;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Quilt Loader in a Loom project. Loom reads the loader's libraries from {@code fabric-installer.json}; Quilt Loader
 * ships {@code quilt_installer.json} instead, so its libraries (Mixin, ASM, quilt-json5, quilt-config, MixinExtras for
 * development) are read from there and added here: the exact list of that loader version, nothing guessed.
 */
final class QuiltSetup {

    static final String MAVEN = "https://maven.quiltmc.org/repository/release/";
    private static final Pattern LIBRARY = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"url\"\\s*:\\s*\"([^\"]+)\"");
    private static final List<String> SECTIONS = List.of("common", "development");

    private QuiltSetup() {
    }

    static void addLoader(Project project, String modConfig, String version) {
        project.getRepositories().maven(r -> {
            r.setName("Quilt");
            r.setUrl(MAVEN);
            r.content(c -> c.includeGroup("org.quiltmc"));
        });
        String loader = "org.quiltmc:quilt-loader:" + version;
        project.getDependencies().add(modConfig, loader);

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
    }

    /**
     * Loom writes the development settings as {@code fabric.*} properties into {@code launch.cfg}; Quilt Loader reads
     * the same settings as {@code loader.*}. Without them Quilt finds none of the development mods.
     */
    static void mirrorLaunchProperties(Project project) {
        // Just before each game run: the launch.cfg path is in the run's JVM arguments (-Dfabric.dli.config=...).
        project.getTasks().withType(org.gradle.api.tasks.JavaExec.class).configureEach(t -> t.doFirst(x -> {
            for (String arg : t.getAllJvmArgs()) {
                if (arg.startsWith("-Dfabric.dli.config=")) {
                    java.nio.file.Path cfg = java.nio.file.Path.of(arg.substring("-Dfabric.dli.config=".length()));
                    if (java.nio.file.Files.isRegularFile(cfg)) {
                        mirror(cfg);
                    }
                }
            }
        }));
    }

    /** Quilt's game entry classes (quilt_installer.json "mainClass"); Loom would pick Fabric Loader's. */
    static void mainClasses(net.fabricmc.loom.api.LoomGradleExtensionAPI loom, Project project) {
        loom.getRuns().configureEach(r -> r.getMainClass().set(project.provider(() -> "client".equals(r.getEnvironment())
                ? "org.quiltmc.loader.impl.launch.knot.KnotClient"
                : "org.quiltmc.loader.impl.launch.knot.KnotServer")));
    }

    static void mirror(java.nio.file.Path cfg) {
        try {
            List<String> out = new ArrayList<>();
            for (String line : java.nio.file.Files.readAllLines(cfg)) {
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
            java.nio.file.Files.write(cfg, out);
        } catch (IOException e) {
            throw new GradleException("Cannot update " + cfg, e);
        }
    }

    private static final List<String> MIRRORED = List.of("development", "remapClasspathFile", "defaultMixinRemapType",
            "classPathGroups");

    /** {@code group:name:version} of the {@code common} and {@code development} libraries of the installer json. */
    static List<String> libraries(File loaderJar) {
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

    static List<String> parse(String json) {
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
