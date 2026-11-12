package lv.cebbys.mcmods.modularity.loom.guest;

import lv.cebbys.mcmods.modularity.platform.guest.GuestDescriptor;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Finds guest mods: every {@code *.jar} in {@code mods/} and every path in the system property
 * {@code modularity.guestMods} (separated by the path separator; jars or folders, for development). A candidate is a
 * guest when it has a {@code modularity.mod.json} and no {@code fabric.mod.json}.
 *
 * <p>Fabric and Quilt Loader both provide the Fabric Loader API, so the same scan runs on both. Only the Fabric launch
 * turns the guests into mods (docs/08).
 */
public final class GuestCandidates {
    public static final String PROPERTY = "modularity.guestMods";

    private GuestCandidates() {
    }

    /** All guests, in path order; the same mod twice is possible and is resolved by the caller. */
    public static List<GuestDescriptor> find() {
        List<GuestDescriptor> guests = new ArrayList<>();
        for (Path candidate : candidates()) {
            try {
                read(candidate).ifPresent(guests::add);
            } catch (IOException e) {
                throw new UncheckedIOException("Modularity: cannot read guest mod " + candidate, e);
            }
        }
        return guests;
    }

    private static List<Path> candidates() {
        List<Path> paths = new ArrayList<>();
        Path mods = FabricLoader.getInstance().getGameDir().resolve("mods");
        if (Files.isDirectory(mods)) {
            try (Stream<Path> files = Files.list(mods)) {
                files.filter(p -> p.getFileName().toString().endsWith(".jar")).sorted().forEach(paths::add);
            } catch (IOException e) {
                throw new UncheckedIOException("Modularity: cannot list " + mods, e);
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
