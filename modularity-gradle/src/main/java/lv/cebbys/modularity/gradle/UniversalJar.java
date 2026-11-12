package lv.cebbys.modularity.gradle;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Merges the platform jars into the one universal jar: every loader's class set, moved to its own package so that
 * the intermediary-mapped (Fabric, Quilt) and official-mapped (Forge, NeoForge) trees can coexist, plus one metadata
 * file per loader (docs/07).
 */
public final class UniversalJar {

    /** Package root of the mod's own classes in every platform jar. */
    public static final String ROOT = "lv/cebbys/mcmods/modularity";
    private static final String RELOCATED = ROOT + "/universal/";
    /**
     * The API lives in its own module, nested in the universal jar and loaded as a mod under this real package
     * (Fabric: {@code META-INF/jars}, Forge/NeoForge: {@code META-INF/jarjar}). Relocating it would leave the
     * platform classes referring to a package nothing provides.
     */
    private static final String API = ROOT + "/api/";
    private static final String MANIFEST = "META-INF/MANIFEST.MF";

    private UniversalJar() {
    }

    /** One platform jar, the loader whose relocated class tree it contributes, and its generated mixin config. */
    record Part(Path jar, Loader loader, String mixinConfigName) {
    }

    /**
     * @param mixinPackage the table's {@code [mod] mixin-package}, rewritten for the relocated tree of each part
     */
    static void write(Path output, List<Part> parts, String mixinPackage) throws IOException {
        Files.createDirectories(output.getParent());
        Set<String> written = new HashSet<>();
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(output))) {
            write(zip, written, MANIFEST, "Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8));
            for (Part part : parts) {
                String metadata = switch (part.loader()) {
                    case FABRIC -> "fabric.mod.json";
                    case QUILT -> "quilt.mod.json";
                    case NEOFORGE, FORGE -> null;
                };
                try (ZipFile in = new ZipFile(part.jar().toFile())) {
                    for (ZipEntry entry : Collections.list(in.entries())) {
                        String name = entry.getName();
                        if (entry.isDirectory() || name.equals(MANIFEST)) {
                            continue;
                        }
                        byte[] bytes = in.getInputStream(entry).readAllBytes();
                        if (name.endsWith(".class") && name.startsWith(ROOT + "/")) {
                            write(zip, written, relocatedPath(name, part.loader()), relocate(bytes, part.loader()));
                        } else if (name.equals(part.mixinConfigName())) {
                            // Only the package moves; the classes in the config are relative to it.
                            write(zip, written, name, replace(bytes, mixinPackage,
                                    relocatedPackage(mixinPackage, part.loader())));
                        } else if (name.equals(metadata)) {
                            // Entry points are the only class names in the metadata; the mixin config is referenced
                            // by file name.
                            write(zip, written, name, replace(bytes, ROOT.replace('/', '.') + ".",
                                    relocatedPackage(ROOT.replace('/', '.'), part.loader()) + "."));
                        } else if (name.startsWith("META-INF/services/")) {
                            // A service file names a class, and that class moves with its tree. The interface in the
                            // file name is a loader API and stays (Forge: the locator service, docs/07 7.8).
                            write(zip, written, name, replace(bytes, ROOT.replace('/', '.') + ".",
                                    relocatedPackage(ROOT.replace('/', '.'), part.loader()) + "."));
                        } else {
                            // Resources (icon, pack.mcmeta, mods.toml files, nested jars) are loader-neutral or have
                            // per-loader names; the first one wins, so a shared icon is stored once.
                            write(zip, written, name, bytes);
                        }
                    }
                }
            }
        }
    }

    /** {@code lv/cebbys/mcmods/modularity/fabric/X} -> {@code lv/cebbys/mcmods/modularity/universal/<loader>/fabric/X}. */
    static String relocatedPath(String path, Loader loader) {
        return relocatable(path) ? RELOCATED + loader.id() + path.substring(ROOT.length()) : path;
    }

    /** Only the platform's own classes move: the API keeps its package, the nested jar provides it. */
    private static boolean relocatable(String internalName) {
        return (internalName.equals(ROOT) || internalName.startsWith(ROOT + "/"))
                && !internalName.startsWith(API);
    }

    /** Dotted package of one loader's relocated tree, e.g. the mixin package of the Fabric classes. */
    static String relocatedPackage(String pkg, Loader loader) {
        String from = ROOT.replace('/', '.');
        return pkg.startsWith(from) ? from + ".universal." + loader.id() + pkg.substring(from.length()) : pkg;
    }

    /** Replaces the mod's own class references so the tree can live under a new package. Minecraft is untouched. */
    static byte[] relocate(byte[] classBytes, Loader loader) {
        String from = ROOT;
        String to = RELOCATED + loader.id();
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(classBytes).accept(new ClassRemapper(writer, new Remapper() {
            @Override
            public String map(String internalName) {
                return relocatable(internalName) ? to + internalName.substring(from.length()) : internalName;
            }
        }), 0);
        return writer.toByteArray();
    }

    private static byte[] replace(byte[] bytes, String from, String to) {
        return new String(bytes, StandardCharsets.UTF_8).replace(from, to).getBytes(StandardCharsets.UTF_8);
    }

    private static void write(ZipOutputStream zip, Set<String> written, String name, byte[] bytes) throws IOException {
        if (!written.add(name)) {
            return;
        }
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }
}
