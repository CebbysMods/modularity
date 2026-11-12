package lv.cebbys.mcmods.modularity.forge.locating;

import net.minecraftforge.forgespi.locating.IDependencyLocator;
import net.minecraftforge.forgespi.locating.IModFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * An empty dependency locator. Its task is the service file that names it, not the mods it finds.
 *
 * <p>FML adds a jar in {@code mods/} to ModLauncher only when the jar's module descriptor provides
 * {@code cpw.mods.modlauncher.api.ITransformationService},
 * {@code net.minecraftforge.forgespi.locating.IModLocator} or
 * {@code net.minecraftforge.forgespi.locating.IDependencyLocator} (the {@code SERVICES} constant of
 * {@code ModDirTransformerDiscoverer}). ModLauncher then offers the jar to every launch plugin, and the Mixin
 * launch plugin reads the {@code MixinConfigs} attribute of the jar manifest. A Modularity jar provides no such
 * service, so Mixin never sees the attribute (docs/07 7.8, bean modularity-6rm0).
 *
 * <p>This locator claims no mod: {@link #isValid} returns false and {@link #scanMods} returns an empty list, so
 * the mods of the game come from FML as before. A development run does not use it, because ForgeGradle adds a
 * class folder and the discoverer visits files only.
 */
public final class ModularityForgeLocator implements IDependencyLocator {

    @Override
    public String name() {
        return "modularity";
    }

    /** Unused: the locator claims no file, so FML never calls this method. */
    @Override
    public void scanFile(IModFile file, Consumer<Path> pathConsumer) {
    }

    @Override
    public void initArguments(Map<String, ?> arguments) {
    }

    @Override
    public boolean isValid(IModFile file) {
        return false;
    }

    @Override
    public List<IModFile> scanMods(Iterable<IModFile> loadedMods) {
        return List.of();
    }
}
