package lv.cebbys.modularity.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * One jar for every loader of one Minecraft version (docs/07): the platform jars of the version merged into
 * {@code modularity-<minecraft>-<version>.jar}, each class set relocated so the different mapping families can live
 * together, with the metadata file of every loader.
 */
@CacheableTask
public abstract class UniversalJarTask extends DefaultTask {

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getTableFile();

    /** Platform name in the table, e.g. {@code 21.1}. */
    @Input
    public abstract Property<String> getPlatform();

    /** Empty for a loader the version has no enabled platform for. */
    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getFabricJar();

    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getForgeJar();

    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getNeoForgeJar();

    @OutputFile
    public abstract RegularFileProperty getOutputJar();

    @TaskAction
    public void assemble() {
        PlatformTable table = PlatformTable.read(getTableFile().get().getAsFile().toPath());
        if (table.version(getPlatform().get()).isEmpty()) {
            throw new IllegalStateException("No Minecraft version " + getPlatform().get() + " in "
                    + PlatformTable.FILE);
        }
        List<UniversalJar.Part> parts = new ArrayList<>();
        add(parts, getFabricJar(), Loader.FABRIC);
        add(parts, getForgeJar(), Loader.FORGE);
        add(parts, getNeoForgeJar(), Loader.NEOFORGE);
        if (parts.isEmpty()) {
            throw new IllegalStateException("No platform jar for " + getPlatform().get());
        }
        Path output = getOutputJar().get().getAsFile().toPath();
        try {
            UniversalJar.write(output, parts, table.mod().mixinPackage(), table.mod().id() + ".mixins.json");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        getLogger().lifecycle("Universal jar {} ({} class sets)", output,
                parts.stream().map(p -> p.loader().id()).toList());
    }

    private static void add(List<UniversalJar.Part> parts, ConfigurableFileCollection jars, Loader loader) {
        if (!jars.isEmpty()) {
            parts.add(new UniversalJar.Part(jars.getSingleFile().toPath(), loader));
        }
    }
}
