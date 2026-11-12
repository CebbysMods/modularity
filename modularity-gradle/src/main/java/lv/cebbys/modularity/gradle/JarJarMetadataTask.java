package lv.cebbys.modularity.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes {@code META-INF/jarjar/metadata.json}, the index of the jars nested inside a Forge mod jar. ForgeGradle 7
 * does not write it, so the nested modules the {@code jar} task copies into {@code META-INF/jarjar} are invisible
 * without it.
 *
 * <p>One entry per module, given as {@code group:artifact:version}; the nested file name follows ModDevGradle's
 * convention, {@code group.artifact-version.jar}.
 */
@CacheableTask
public abstract class JarJarMetadataTask extends DefaultTask {

    /** The coordinates of the nested modules, {@code group:artifact:version} each. */
    @Input
    public abstract ListProperty<String> getEntries();

    /** The directory the task writes {@code META-INF/jarjar/metadata.json} into. */
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
