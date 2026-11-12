package lv.cebbys.modularity.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

/** Writes the loader metadata and the mixin config of one platform (see {@link ModMetadata}). */
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

    /** The mod icon ({@code [mod] icon}), copied to {@link PlatformTable.Mod#iconPath()}. */
    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getIconFile();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void generate() {
        PlatformTable table = PlatformTable.read(getTableFile().get().getAsFile().toPath());
        PlatformTable.Platform platform = table.platform(getPlatform().get()).orElseThrow();
        Path javaDir = getJavaDir().get().getAsFile().toPath();
        List<String> mixins = ModMetadata.findMixins(javaDir, table.mod().mixinPackage());
        Map<EntryPoint, List<String>> entrypoints = ModMetadata.findEntrypoints(javaDir);
        Path out = getOutputDir().get().getAsFile().toPath();
        try {
            Files.createDirectories(out);
            if (getIconFile().isPresent()) {
                Path icon = out.resolve(table.mod().iconPath());
                Files.createDirectories(icon.getParent());
                Files.copy(getIconFile().get().getAsFile().toPath(), icon, StandardCopyOption.REPLACE_EXISTING);
            }
            if (platform.metadataFormat() == PlatformTable.MetadataFormat.NEOFORGE
                    || platform.metadataFormat() == PlatformTable.MetadataFormat.FORGE) {
                // The shared Minecraft layer carries the mixins. Both Forge-family loaders run in the official
                // namespace, so their configs need no refmap and are written with the metadata. NeoForge reads the
                // [[mixins]] block of neoforge.mods.toml. Forge has no such block: its jar names the same config in
                // the MixinConfigs manifest attribute (ForgePlatformSetup), and the block stays in mods.toml so the
                // name has one source.
                boolean declared = !mixins.isEmpty();
                Path toml = out.resolve(ModMetadata.metadataPath(platform));
                Files.createDirectories(toml.getParent());
                Files.writeString(toml, ModMetadata.modsToml(table.mod(), platform, getModVersion().get(), declared));
                if (declared) {
                    Files.writeString(out.resolve(ModMetadata.mixinConfigName(table.mod(), platform.loader())),
                            ModMetadata.mixinConfig(table.mod(), platform, mixins));
                }
                if (platform.metadataFormat() == PlatformTable.MetadataFormat.FORGE) {
                    // Forge reads the config from the MixinConfigs manifest attribute of the mod file. A production
                    // jar gets it from the jar task (ForgePlatformSetup). A development run scans the output directory
                    // as a mod file and reads the manifest of that directory, so the attribute goes there too
                    // (docs/07 7.8).
                    Path manifest = out.resolve("META-INF/MANIFEST.MF");
                    if (declared) {
                        Files.createDirectories(manifest.getParent());
                        Files.writeString(manifest, "Manifest-Version: 1.0\n"
                                + ModMetadata.MANIFEST_MIXIN_CONFIGS + ": "
                                + ModMetadata.mixinConfigName(table.mod(), platform.loader()) + "\n\n");
                    } else {
                        Files.deleteIfExists(manifest);
                    }
                }
                return;
            }
            getLogger().info("{}: entrypoints {}", platform.id(),
                    ModMetadata.loaderEntrypoints(entrypoints, platform.loader()));
            if (platform.metadataFormat() == PlatformTable.MetadataFormat.QUILT) {
                Files.writeString(out.resolve(ModMetadata.metadataPath(platform)),
                        ModMetadata.quiltModJson(table.mod(), platform, getModVersion().get(),
                                !mixins.isEmpty(), entrypoints));
            } else {
                Files.writeString(out.resolve(ModMetadata.metadataPath(platform)),
                        ModMetadata.fabricModJson(table.mod(), platform, table.fabricLoaderMin(), getModVersion().get(),
                                !mixins.isEmpty(), entrypoints));
            }
            Path mixinFile = out.resolve(ModMetadata.mixinConfigName(table.mod(), platform.loader()));
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
