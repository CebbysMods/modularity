package lv.cebbys.mcmods.modularity.fabric.guest;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Loads guest mods: jars (or folders) with a {@code modularity.mod.json} and no {@code fabric.mod.json}, from the mods
 * folder and from the paths in the system property {@code modularity.guestMods} (development). Each becomes a real
 * Fabric mod before the game starts; its {@code modularity} entrypoints then run with all other Modularity mods.
 */
public final class GuestMods implements PreLaunchEntrypoint {
    public static final String PROPERTY = "modularity.guestMods";
    private static final Logger LOGGER = LoggerFactory.getLogger("modularity");

    @Override
    public void onPreLaunch() {
        List<GuestDescriptor> guests = new ArrayList<>();
        for (Path candidate : candidates()) {
            try {
                read(candidate).ifPresent(guests::add);
            } catch (IOException e) {
                throw new IllegalStateException("Modularity: cannot read guest mod " + candidate, e);
            }
        }
        if (!guests.isEmpty() && onQuilt()) {
            // LoaderInjection uses Fabric Loader internals, which Quilt Loader does not have (docs/08).
            LOGGER.error("[modularity] Guest mods are not supported on Quilt yet; not loaded: {}",
                    guests.stream().map(GuestDescriptor::id).toList());
            return;
        }
        Map<String, GuestDescriptor> byId = new LinkedHashMap<>();
        for (GuestDescriptor guest : guests) {
            if (FabricLoader.getInstance().isModLoaded(guest.id()) || byId.containsKey(guest.id())) {
                throw new IllegalStateException("Modularity: guest mod '" + guest.id() + "' (" + guest.origin()
                        + ") is already loaded");
            }
            byId.put(guest.id(), guest);
        }
        for (GuestDescriptor guest : byId.values()) {
            try {
                LoaderInjection.addMod(guest.origin(), guest.toFabricModJson().toString());
            } catch (Exception e) {
                throw new IllegalStateException("Modularity: cannot add guest mod '" + guest.id() + "'", e);
            }
            LOGGER.info("[modularity] Loaded guest mod {} {} from {}", guest.id(), guest.version(),
                    guest.origin().getFileName());
        }
        byId.values().forEach(GuestMods::checkDependencies);
    }

    private static boolean onQuilt() {
        try {
            Class.forName("org.quiltmc.loader.api.QuiltLoader", false, GuestMods.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /** Fabric resolved dependencies before guests existed; the same rules, checked here. */
    private static void checkDependencies(GuestDescriptor guest) {
        List<String> problems = new ArrayList<>();
        guest.depends().forEach((id, range) -> {
            Optional<ModContainer> mod = FabricLoader.getInstance().getModContainer(id);
            if (mod.isEmpty()) {
                problems.add("requires " + id + " " + range + ", which is missing");
            } else if (!matches(range, mod.get())) {
                problems.add("requires " + id + " " + range + ", found "
                        + mod.get().getMetadata().getVersion().getFriendlyString());
            }
        });
        guest.map("breaks").forEach((id, range) -> FabricLoader.getInstance().getModContainer(id)
                .filter(mod -> matches(range, mod))
                .ifPresent(mod -> problems.add("is incompatible with " + id + " "
                        + mod.getMetadata().getVersion().getFriendlyString())));
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Modularity: guest mod '" + guest.id() + "' "
                    + String.join("; ", problems));
        }
    }

    private static boolean matches(String range, ModContainer mod) {
        try {
            return VersionPredicate.parse(range).test(mod.getMetadata().getVersion());
        } catch (Exception e) {
            throw new IllegalStateException("Modularity: invalid version range '" + range + "'", e);
        }
    }

    private static List<Path> candidates() {
        List<Path> paths = new ArrayList<>();
        Path mods = FabricLoader.getInstance().getGameDir().resolve("mods");
        if (Files.isDirectory(mods)) {
            try (Stream<Path> files = Files.list(mods)) {
                files.filter(p -> p.getFileName().toString().endsWith(".jar")).sorted().forEach(paths::add);
            } catch (IOException e) {
                throw new IllegalStateException("Modularity: cannot list " + mods, e);
            }
        }
        String extra = System.getProperty(PROPERTY);
        if (extra != null && !extra.isBlank()) {
            for (String p : extra.split(File.pathSeparator)) {
                paths.add(Path.of(p.trim()));
            }
        }
        return paths;
    }

    /** The descriptor of {@code path}, or empty when it is no guest mod (no descriptor, or a Fabric mod). */
    private static Optional<GuestDescriptor> read(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            return read(path, path);
        }
        try (FileSystem jar = FileSystems.newFileSystem(path)) {
            return read(path, jar.getPath("/"));
        }
    }

    private static Optional<GuestDescriptor> read(Path origin, Path root) throws IOException {
        Path descriptor = root.resolve(GuestDescriptor.FILE);
        if (!Files.exists(descriptor) || Files.exists(root.resolve("fabric.mod.json"))) {
            return Optional.empty();
        }
        return Optional.of(GuestDescriptor.read(origin, descriptor));
    }
}
