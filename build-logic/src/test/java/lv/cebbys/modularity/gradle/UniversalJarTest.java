package lv.cebbys.modularity.gradle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The merge of the platform jars into the one universal jar. */
class UniversalJarTest {

    private static final String ROOT = "lv/cebbys/mcmods/modularity";
    private static final String MIXIN_CONFIG = "modularity.mixins.json";

    @Test
    void everyLoaderGetsItsOwnRelocatedClassSet(@TempDir Path dir) throws IOException {
        Path fabricJar = platformJar(dir.resolve("fabric.jar"));
        Path forgeJar = platformJar(dir.resolve("forge.jar"));
        Path out = dir.resolve("universal.jar");

        UniversalJar.write(out, List.of(
                new UniversalJar.Part(fabricJar, Loader.FABRIC, MIXIN_CONFIG),
                new UniversalJar.Part(forgeJar, Loader.FORGE, "modularity.forge.mixins.json")),
                "lv.cebbys.mcmods.modularity.mixin");

        Map<String, byte[]> entries = read(out);
        // Two class sets, one per mapping family: the same class in two packages, the original paths gone.
        assertTrue(entries.containsKey(ROOT + "/universal/fabric/Target.class"));
        assertTrue(entries.containsKey(ROOT + "/universal/fabric/User.class"));
        assertTrue(entries.containsKey(ROOT + "/universal/forge/Target.class"));
        assertTrue(entries.containsKey(ROOT + "/universal/forge/User.class"));
        assertFalse(entries.containsKey(ROOT + "/Target.class"));
        // The references inside the class follow the relocation.
        assertTrue(new String(entries.get(ROOT + "/universal/forge/User.class"), StandardCharsets.ISO_8859_1)
                .contains(ROOT + "/universal/forge/Target"));
        // One metadata file per loader, the shared resources stored once.
        assertTrue(entries.containsKey("fabric.mod.json"));
        assertTrue(entries.containsKey("META-INF/mods.toml"));
        assertEquals(1, entries.keySet().stream().filter(n -> n.equals("assets/modularity/icon.png")).count());
        assertNotNull(entries.get("META-INF/MANIFEST.MF"));
    }

    @Test
    void fabricEntrypointsAndMixinPackageFollowTheRelocatedTree(@TempDir Path dir) throws IOException {
        Path out = dir.resolve("universal.jar");
        UniversalJar.write(out, List.of(
                new UniversalJar.Part(platformJar(dir.resolve("fabric.jar")), Loader.FABRIC, MIXIN_CONFIG)),
                "lv.cebbys.mcmods.modularity.mixin");

        Map<String, byte[]> entries = read(out);
        String fabricMod = new String(entries.get("fabric.mod.json"), StandardCharsets.UTF_8);
        assertTrue(fabricMod.contains("\"lv.cebbys.mcmods.modularity.universal.fabric.Modularity\""), fabricMod);
        String mixins = new String(entries.get(MIXIN_CONFIG), StandardCharsets.UTF_8);
        assertTrue(mixins.contains("lv.cebbys.mcmods.modularity.universal.fabric.mixin"), mixins);
    }

    @Test
    void quiltGetsItsOwnTreeMetadataAndMixinConfig(@TempDir Path dir) throws IOException {
        Path fabricJar = platformJar(dir.resolve("fabric.jar"));
        Path quiltJar = quiltPlatformJar(dir.resolve("quilt.jar"));
        Path out = dir.resolve("universal.jar");

        UniversalJar.write(out, List.of(
                new UniversalJar.Part(fabricJar, Loader.FABRIC, MIXIN_CONFIG),
                new UniversalJar.Part(quiltJar, Loader.QUILT, "modularity.quilt.mixins.json")),
                "lv.cebbys.mcmods.modularity.mixin");

        Map<String, byte[]> entries = read(out);
        assertTrue(entries.containsKey(ROOT + "/universal/fabric/Target.class"));
        assertTrue(entries.containsKey(ROOT + "/universal/quilt/Target.class"));
        // Both metadata files and both mixin configs survive, each rewritten for its own tree.
        assertTrue(entries.containsKey("fabric.mod.json"));
        assertTrue(entries.containsKey("quilt.mod.json"));
        assertTrue(entries.containsKey(MIXIN_CONFIG));
        assertTrue(entries.containsKey("modularity.quilt.mixins.json"));
        String fabricMod = new String(entries.get("fabric.mod.json"), StandardCharsets.UTF_8);
        assertTrue(fabricMod.contains("\"lv.cebbys.mcmods.modularity.universal.fabric.Modularity\""), fabricMod);
        String quiltMod = new String(entries.get("quilt.mod.json"), StandardCharsets.UTF_8);
        assertTrue(quiltMod.contains("\"lv.cebbys.mcmods.modularity.universal.quilt.Modularity\""), quiltMod);
        String fabricMixins = new String(entries.get(MIXIN_CONFIG), StandardCharsets.UTF_8);
        assertTrue(fabricMixins.contains("lv.cebbys.mcmods.modularity.universal.fabric.mixin"), fabricMixins);
        String quiltMixins = new String(entries.get("modularity.quilt.mixins.json"), StandardCharsets.UTF_8);
        assertTrue(quiltMixins.contains("lv.cebbys.mcmods.modularity.universal.quilt.mixin"), quiltMixins);
        // Shared resources are still stored once.
        assertEquals(1, entries.keySet().stream().filter(n -> n.equals("assets/modularity/icon.png")).count());
    }

    /** A Quilt platform jar: its own metadata file and mixin config, plus one class and a shared resource. */
    private static Path quiltPlatformJar(Path file) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put(ROOT + "/Target.class", bytes("Target"));
        entries.put("quilt.mod.json", """
                {"quilt_loader":{"entrypoints":{"main":["lv.cebbys.mcmods.modularity.Modularity"]}}}"""
                .getBytes(StandardCharsets.UTF_8));
        entries.put("modularity.quilt.mixins.json", """
                {"package":"lv.cebbys.mcmods.modularity.mixin"}
                """.getBytes(StandardCharsets.UTF_8));
        entries.put("assets/modularity/icon.png", new byte[] {1, 2, 3});
        write(file, entries);
        return file;
    }

    @Test
    void onlyClassesOfTheModAreRelocated() {
        assertEquals(ROOT + "/universal/fabric/Target", UniversalJar.relocatedPath(ROOT + "/Target", Loader.FABRIC));
        // The API is a separate nested mod: its package must not move, or the platform classes would point at a
        // package nothing provides (the production client then fails with NoClassDefFoundError).
        assertEquals(ROOT + "/api/Modularity", UniversalJar.relocatedPath(ROOT + "/api/Modularity", Loader.FABRIC));
        // Other roots and the loader's own metadata are copied as they are.
        assertEquals("net/minecraft/class_1", UniversalJar.relocatedPath("net/minecraft/class_1", Loader.FABRIC));
        assertEquals("lv.cebbys.mcmods.modularity.universal.neoforge.mixin",
                UniversalJar.relocatedPackage("lv.cebbys.mcmods.modularity.mixin", Loader.NEOFORGE));
    }

    @Test
    void apiClassesAndReferencesKeepTheirPackage(@TempDir Path dir) throws IOException {
        Path jar = dir.resolve("fabric.jar");
        write(jar, Map.of(
                ROOT + "/api/Modularity.class", plainClass(ROOT + "/api/Modularity"),
                // A platform class with a field of an API type: the reference must survive the relocation.
                ROOT + "/platform/Core.class", classWithField(ROOT + "/platform/Core", "api", "L" + ROOT + "/api/Modularity;")));
        Path out = dir.resolve("universal.jar");
        UniversalJar.write(out, List.of(
                new UniversalJar.Part(jar, Loader.FABRIC, MIXIN_CONFIG)),
                "lv.cebbys.mcmods.modularity.mixin");

        Map<String, byte[]> entries = read(out);
        assertTrue(entries.containsKey(ROOT + "/api/Modularity.class"));
        assertFalse(entries.containsKey(ROOT + "/universal/fabric/api/Modularity.class"));
        assertTrue(entries.containsKey(ROOT + "/universal/fabric/platform/Core.class"));
        String core = new String(entries.get(ROOT + "/universal/fabric/platform/Core.class"), StandardCharsets.ISO_8859_1);
        assertTrue(core.contains("L" + ROOT + "/api/Modularity;"), core);
        assertFalse(core.contains("universal/fabric/api"), core);
        assertTrue(entries.keySet().stream().noneMatch(name -> name.startsWith(ROOT + "/universal/") && name.contains("/api/")));
    }

    /** A platform jar with one class of the mod, a class referring to it, both metadata files and a resource. */
    private static Path platformJar(Path file) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put(ROOT + "/Target.class", bytes("Target"));
        entries.put(ROOT + "/User.class", bytes("User"));
        entries.put("fabric.mod.json", """
                {"entrypoints":{"main":["lv.cebbys.mcmods.modularity.Modularity"]}}
                """.getBytes(StandardCharsets.UTF_8));
        entries.put("META-INF/mods.toml", "modLoader=\"javafml\"\n".getBytes(StandardCharsets.UTF_8));
        entries.put(MIXIN_CONFIG, """
                {"package":"lv.cebbys.mcmods.modularity.mixin"}
                """.getBytes(StandardCharsets.UTF_8));
        entries.put("assets/modularity/icon.png", new byte[] {1, 2, 3});
        entries.put("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8));
        Files.createDirectories(file.getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return file;
    }

    /** {@code User} has a field of type {@code Target}, so the reference shows in its bytes. */
    private static byte[] bytes(String name) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, ROOT + "/" + name, null, "java/lang/Object", null);
        if (name.equals("User")) {
            writer.visitField(Opcodes.ACC_PRIVATE, "target", "L" + ROOT + "/Target;", null, null);
        }
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] plainClass(String internalName) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] classWithField(String internalName, String fieldName, String fieldType) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);
        writer.visitField(Opcodes.ACC_PRIVATE, fieldName, fieldType, null, null);
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static void write(Path file, Map<String, byte[]> entries) throws IOException {
        Files.createDirectories(file.getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
    }

    private static Map<String, byte[]> read(Path jar) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            for (ZipEntry entry : Collections.list(zip.entries())) {
                if (!entry.isDirectory()) {
                    try (InputStream in = zip.getInputStream(entry)) {
                        entries.put(entry.getName(), in.readAllBytes());
                    }
                }
            }
        }
        return entries;
    }
}
