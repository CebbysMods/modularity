package lv.cebbys.mcmods.modularity.fabric.guest;

import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.ModContainerImpl;
import net.fabricmc.loader.impl.discovery.ModCandidateImpl;
import net.fabricmc.loader.impl.entrypoint.EntrypointStorage;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.metadata.DependencyOverrides;
import net.fabricmc.loader.impl.metadata.EntrypointMetadata;
import net.fabricmc.loader.impl.metadata.LoaderModMetadata;
import net.fabricmc.loader.impl.metadata.ModMetadataParser;
import net.fabricmc.loader.impl.metadata.VersionOverrides;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Adds a mod to Fabric Loader after discovery, as if it had been found in the mods folder: Fabric parses the
 * generated {@code fabric.mod.json}, the jar joins the game class path, the container joins the mod list (mod menu,
 * resource and data packs, {@code isModLoaded}) and its entrypoints are registered.
 *
 * <p>Fabric Loader has no public API for this; this class is the only place that touches loader internals
 * (checked against Fabric Loader 0.20.1). If a loader version changes them, only this class changes.
 */
public final class LoaderInjection {
    private LoaderInjection() {
    }

    public static void addMod(Path origin, String fabricModJson) throws Exception {
        FabricLoaderImpl loader = FabricLoaderImpl.INSTANCE;
        LoaderModMetadata metadata = ModMetadataParser.parseMetadata(
                new ByteArrayInputStream(fabricModJson.getBytes(StandardCharsets.UTF_8)), origin.toString(),
                List.of(), new VersionOverrides(), new DependencyOverrides(loader.getConfigDir()), false);

        Method createPlain = ModCandidateImpl.class.getDeclaredMethod("createPlain", List.class,
                LoaderModMetadata.class, boolean.class, Collection.class);
        createPlain.setAccessible(true);
        ModCandidateImpl candidate = (ModCandidateImpl) createPlain.invoke(null, List.of(origin), metadata, false,
                List.of());
        ModContainerImpl container = new ModContainerImpl(candidate);

        FabricLauncherBase.getLauncher().addToClassPath(origin);
        List<ModContainerImpl> mods = field(loader, "mods");
        Map<String, ModContainerImpl> modMap = field(loader, "modMap");
        mods.add(container);
        modMap.put(metadata.getId(), container);
        for (String provided : metadata.getProvides()) {
            modMap.put(provided, container);
        }

        EntrypointStorage entrypoints = field(loader, "entrypointStorage");
        Map<String, LanguageAdapter> adapters = field(loader, "adapterMap");
        for (String key : metadata.getEntrypointKeys()) {
            for (EntrypointMetadata entry : metadata.getEntrypoints(key)) {
                entrypoints.add(container, key, entry, adapters);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(Object owner, String name) throws ReflectiveOperationException {
        Field f = FabricLoaderImpl.class.getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(owner);
    }
}
