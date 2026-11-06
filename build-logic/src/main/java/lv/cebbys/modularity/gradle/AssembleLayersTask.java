package lv.cebbys.modularity.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Copies the winning file of every path of the layers into one directory (see {@link Layers}). */
@CacheableTask
public abstract class AssembleLayersTask extends DefaultTask {

    /** Layer root, e.g. {@code platforms/fabric} (holds common, changes, versions). */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getBaseDir();

    @Input
    public abstract Property<String> getMinecraft();

    /** Path inside each layer, e.g. {@code src/main}. */
    @Input
    public abstract Property<String> getSourceSet();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    /** {@code <path> <- <layer>} per file, to trace compiler errors back to the source layer. */
    @OutputFile
    public abstract RegularFileProperty getOriginsFile();

    @TaskAction
    public void assemble() {
        McVersion version = McVersion.parse(getMinecraft().get());
        List<Layers.Layer> layers = Layers.resolve(getBaseDir().get().getAsFile().toPath(), version,
                getSourceSet().get());
        Map<String, Layers.Origin> origins;
        try {
            origins = Layers.assemble(layers, getOutputDir().get().getAsFile().toPath());
        } catch (Layers.LayerException e) {
            throw new org.gradle.api.GradleException("Layer check failed for " + version + ":\n" + e.getMessage(), e);
        }
        String text = "# layers: " + layers.stream().map(Layers.Layer::label).collect(Collectors.joining(", ")) + "\n"
                + origins.entrySet().stream().map(e -> e.getKey() + " <- " + e.getValue().layer())
                .collect(Collectors.joining("\n")) + "\n";
        try {
            Path file = getOriginsFile().get().getAsFile().toPath();
            Files.createDirectories(file.getParent());
            Files.writeString(file, text);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        getLogger().info("{}: {}", version, Layers.counts(layers));
    }
}
