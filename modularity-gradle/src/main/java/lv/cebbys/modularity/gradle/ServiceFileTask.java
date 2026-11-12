package lv.cebbys.modularity.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes one {@code META-INF/services/<interface>} file into a generated directory, so a jar task can carry it
 * and the source set cannot (docs/07, 7.8).
 *
 * <p>The distinction matters on Forge. FML reads a class-path entry that names a locator service as a transformer
 * artifact and stops reading it as a mod, so a service file in {@code src/main/resources} makes the development run
 * lose Modularity itself: {@code Mod 'modularity_smoke' requires 'modularity' … not installed}.</p>
 */
public abstract class ServiceFileTask extends DefaultTask {

    /** The service interface, which is the file name under {@code META-INF/services/}. */
    @Input
    public abstract Property<String> getService();

    /** The implementation class name written into the file, at the package the jar keeps. */
    @Input
    public abstract Property<String> getProvider();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void write() {
        Path file = getOutputDir().get().getAsFile().toPath()
                .resolve("META-INF/services").resolve(getService().get());
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, getProvider().get() + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
