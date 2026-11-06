package lv.cebbys.modularity.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Writes {@code fabric.mod.json} and the mixin config of one platform (see {@link ModMetadata}). */
@CacheableTask
public abstract class GenerateModMetadataTask extends DefaultTask {

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getTableFile();

    @Input
    public abstract Property<String> getPlatform();

    @Input
    public abstract Property<String> getModVersion();

    /** Assembled Java sources, scanned for {@code @Mixin} classes. */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getJavaDir();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void generate() {
        PlatformTable table = PlatformTable.read(getTableFile().get().getAsFile().toPath());
        PlatformTable.Platform platform = table.platform(getPlatform().get()).orElseThrow();
        List<String> mixins = ModMetadata.findMixins(getJavaDir().get().getAsFile().toPath(), table.mod().mixinPackage());
        Path out = getOutputDir().get().getAsFile().toPath();
        try {
            Files.createDirectories(out);
            Files.writeString(out.resolve("fabric.mod.json"),
                    ModMetadata.fabricModJson(table.mod(), platform, table.fabricLoaderMin(), getModVersion().get(),
                            !mixins.isEmpty()));
            Path mixinFile = out.resolve(ModMetadata.mixinConfigName(table.mod()));
            if (mixins.isEmpty()) {
                Files.deleteIfExists(mixinFile);
            } else {
                Files.writeString(mixinFile, ModMetadata.mixinConfig(table.mod(), platform, mixins));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
