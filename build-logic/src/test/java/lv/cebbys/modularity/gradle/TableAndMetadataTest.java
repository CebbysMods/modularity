package lv.cebbys.modularity.gradle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableAndMetadataTest {

    private static final String TABLE = """
            [defaults]
            fabric-loader = "0.20.1"   # comment
            fabric-loader-min = "0.16.0"

            [mod]
            id = "modularity"
            group = "lv.cebbys.mcmods"
            name = "Modularity"
            description = "API."
            license = "MIT"
            authors = ["CebbyS"]
            mixin-package = "lv.example.mixin"
            companion-mods = [":a", ":b"]

            [mod.depends]
            mvd = ">=4.0.0"

            [[platform]]
            name = "26.1"
            minecraft = "26.1"
            fabric-api = "0.145.1+26.1"
            java = 25

            [[platform]]
            name = "21.1"
            minecraft = "1.21.1"
            fabric-api = "0.116.17+1.21.1"
            java = 21
            enabled = false
            """;

    @Test
    void parsesTableSortedByVersion() {
        PlatformTable t = PlatformTable.parse(TABLE);
        assertEquals(List.of("fabric-21.1", "fabric-26.1"), t.platforms().stream().map(PlatformTable.Platform::id).toList());
        PlatformTable.Platform p = t.platform("fabric-21.1").orElseThrow();
        assertEquals("0.20.1", p.loaderVersion());
        assertEquals(Loader.FABRIC, p.loader());
        assertEquals(21, p.java());
        assertTrue(p.remap());
        assertEquals("remapJar", p.modJarTask());
        assertEquals("jar", t.platform("fabric-26.1").orElseThrow().modJarTask());
        assertFalse(p.enabled());
        assertFalse(t.platform("fabric-26.1").orElseThrow().remap());
        assertEquals(List.of("fabric-26.1"), t.enabled().stream().map(PlatformTable.Platform::id).toList());
        assertEquals(List.of(":a", ":b"), t.mod().companionMods());
        assertEquals("lv.cebbys.mcmods", t.mod().group());
        assertEquals("modularity", t.mod().archivePrefix());
        assertEquals(":platforms:21.1:21.1-fabric", p.projectPath());
        assertEquals("21.1-fabric", p.projectName());
        assertEquals(":platforms:21.1", p.parentPath());
        assertEquals("platforms/versions/21.1", p.parentDir());
        assertTrue(t.platformByProjectName("21.1-fabric").isPresent());
        assertEquals(List.of(":platforms:21.1", ":platforms:26.1"), t.parentPaths());
    }

    @Test
    void loadersHaveOwnIdsVersionsAndLayerStacks() {
        PlatformTable t = PlatformTable.parse(TABLE + """

                [[platform]]
                name = "21.1"
                loader = "neoforge"
                minecraft = "1.21.1"
                neoforge = "21.1.265"
                java = 21

                [[platform]]
                name = "21.1"
                loader = "quilt"
                minecraft = "1.21.1"
                quilt-loader = "0.30.2"
                fabric-api = "0.116.17+1.21.1"
                java = 21
                """);
        PlatformTable.Platform neo = t.platform("neoforge-21.1").orElseThrow();
        assertEquals("21.1.265", neo.loaderVersion());
        assertFalse(neo.remap(), "NeoForge runs with Mojang names");
        assertEquals("jar", neo.modJarTask());
        assertEquals("platforms/neoforge/versions/21.1", neo.projectDir());
        assertEquals(List.of("minecraft", "common", "neoforge"), neo.loader().layerRoots());
        PlatformTable.Platform quilt = t.platform("quilt-21.1").orElseThrow();
        assertTrue(quilt.remap());
        assertEquals(List.of("minecraft", "common", "loom", "quilt"), quilt.loader().layerRoots());
        assertThrows(IllegalArgumentException.class, () -> PlatformTable.parse(TABLE.replace("java = 25",
                "java = 25\nloader = \"liteloader\"")), "unknown loader");
        assertThrows(IllegalArgumentException.class, () -> PlatformTable.parse(TABLE + """

                [[platform]]
                name = "21.1"
                loader = "forge"
                minecraft = "1.21.1"
                java = 21
                """), "forge version missing");
    }

    @Test
    void modsTomlForNeoForgeAndForge() {
        PlatformTable t = PlatformTable.parse(TABLE + """

                [[platform]]
                name = "21.1"
                loader = "neoforge"
                minecraft = "1.21.1"
                neoforge = "21.1.265"
                java = 21

                [[platform]]
                name = "21.1"
                loader = "forge"
                minecraft = "1.21.1"
                forge = "52.1.21"
                java = 21
                """);
        String neo = ModMetadata.modsToml(t.mod(), t.platform("neoforge-21.1").orElseThrow(), "0.1.0");
        assertTrue(neo.contains("modId=\"modularity\""), neo);
        assertTrue(neo.contains("modId=\"minecraft\"\ntype=\"required\"\nversionRange=\"[1.21.1]\""), neo);
        assertTrue(neo.contains("modId=\"neoforge\"\ntype=\"required\"\nversionRange=\"[21.1,)\""), neo);
        assertTrue(neo.contains("modId=\"mvd\"\ntype=\"required\"\nversionRange=\"[4.0.0,)\""), neo);
        assertEquals("META-INF/neoforge.mods.toml", ModMetadata.metadataPath(t.platform("neoforge-21.1").orElseThrow()));

        String forge = ModMetadata.modsToml(t.mod(), t.platform("forge-21.1").orElseThrow(), "0.1.0");
        assertTrue(forge.contains("loaderVersion=\"[52,)\""), forge);
        assertTrue(forge.contains("modId=\"forge\"\nmandatory=true\nversionRange=\"[52,)\""), forge);
        assertEquals("META-INF/mods.toml", ModMetadata.metadataPath(t.platform("forge-21.1").orElseThrow()));
        assertFalse(forge.contains("logoFile"), forge);
        PlatformTable withIcon = PlatformTable.parse(TABLE.replace("license = \"MIT\"", "license = \"MIT\"\nicon =\"images/x.png\""));
        assertEquals("assets/modularity/icon.png", withIcon.mod().iconPath());
        assertTrue(ModMetadata.modsToml(withIcon.mod(), t.platform("forge-21.1").orElseThrow(), "0.1.0")
                .contains("logoFile=\"assets/modularity/icon.png\""));
        assertTrue(ModMetadata.fabricModJson(withIcon.mod(), t.platform("fabric-21.1").orElseThrow(), "0.16.0", "0.1.0", false,
                Map.of())
                .contains("\"icon\": \"assets/modularity/icon.png\""));

        assertEquals("[0,)", ModMetadata.mavenRange("*"));
        assertEquals("[1.2.3]", ModMetadata.mavenRange("1.2.3"));
        assertThrows(IllegalArgumentException.class, () -> ModMetadata.mavenRange("~1.2"));
        assertEquals(":utilities:mvd:mvd-neoforge",
                PlatformTable.Mod.companionProject(":utilities:mvd:mvd", Loader.NEOFORGE));
        assertEquals(":utilities:mvd:mvd-fabric",
                PlatformTable.Mod.companionProject(":utilities:mvd:mvd", Loader.QUILT), "Quilt runs the Fabric build");
        assertEquals(":utilities:mvd:mvd-core", PlatformTable.Mod.companionApi(":utilities:mvd:mvd"));
    }

    @Test
    void tableErrors() {
        assertThrows(IllegalArgumentException.class,
                () -> PlatformTable.parse(TABLE.replace("name = \"21.1\"", "name = \"26.1\"")), "duplicate");
        assertThrows(IllegalArgumentException.class,
                () -> PlatformTable.parse(TABLE.replace("minecraft = \"1.21.1\"", "minecraft = \"1.21.2\"")),
                "name does not match version");
        assertThrows(IllegalArgumentException.class, () -> PlatformTable.parse(TABLE.replace("java = 21", "")),
                "missing java");
        assertThrows(IllegalArgumentException.class, () -> MiniToml.parse("a = \"x"));
        assertThrows(IllegalArgumentException.class, () -> MiniToml.parse("a = 1\na = 2"));
    }

    @Test
    void modJsonHasExactMinecraftAndDependencies() {
        PlatformTable t = PlatformTable.parse(TABLE);
        String json = ModMetadata.fabricModJson(t.mod(), t.platform("fabric-21.1").orElseThrow(), t.fabricLoaderMin(), "0.1.0", true,
                Map.of(EntryPoint.COMMON, List.of("lv.example.Main")));
        assertTrue(json.contains("\"minecraft\": \"1.21.1\""), json);
        assertTrue(json.contains("\"mvd\": \">=4.0.0\""), json);
        assertTrue(json.contains("\"fabricloader\": \">=0.16.0\""), json);
        assertTrue(json.contains("\"java\": \">=21\""), json);
        assertTrue(json.contains("\"entrypoints\": {\n    \"main\": [\n      \"lv.example.Main\"\n    ]\n  }"), json);
        assertTrue(json.contains("\"mixins\": [\n    \"modularity.mixins.json\"\n  ]"), json);
        assertTrue(json.contains("\"description\": \"API. Fabric 1.21.1 platform.\""), json);
        assertFalse(ModMetadata.fabricModJson(t.mod(), t.platform("fabric-26.1").orElseThrow(), t.fabricLoaderMin(), "0.1.0", false,
                Map.of())
                .contains("mixins"));
    }

    @Test
    void quiltModJsonHasSchemaGroupMappingsAndNestedJars() {
        PlatformTable t = PlatformTable.parse(TABLE + """

                [[platform]]
                name = "21.1"
                loader = "quilt"
                minecraft = "1.21.1"
                quilt-loader = "0.30.2"
                fabric-api = "0.116.17+1.21.1"
                java = 21
                """);
        PlatformTable.Platform quilt = t.platform("quilt-21.1").orElseThrow();
        String json = ModMetadata.quiltModJson(t.mod(), quilt, "0.1.0", List.of("modularity-api-0.1.0.jar"), true,
                Map.of(EntryPoint.COMMON, List.of("lv.example.Main"),
                        EntryPoint.LAUNCH, List.of("lv.example.Launch")));
        assertTrue(json.contains("\"schema_version\": 1"), json);
        assertTrue(json.contains("\"group\": \"lv.cebbys.mcmods\""), json);
        assertTrue(json.contains("\"id\": \"modularity\""), json);
        assertTrue(json.contains("\"intermediate_mappings\": \"net.fabricmc:intermediary\""), json);
        assertTrue(json.contains("\"main\": [\n        \"lv.example.Main\"\n      ]"), json);
        assertTrue(json.contains("\"pre_launch\": [\n        \"lv.example.Launch\"\n      ]"), json);
        assertTrue(json.contains("\"mixin\": \"modularity.quilt.mixins.json\""), json);
        assertTrue(json.contains("\"jars\": [\n    \"META-INF/jars/modularity-api-0.1.0.jar\"\n  ]"), json);
        assertTrue(json.contains("\"versions\": \"=1.21.1\""), json);
        assertTrue(json.contains("\"versions\": \">=0.30.2\""), json);
        assertTrue(json.contains("\"contributors\": {\n        \"CebbyS\": \"Author\"\n      }"), json);
        assertTrue(json.contains("\"environment\": \"*\""), json);
        // A 26.x platform has no intermediary mappings, so it cannot carry a native Quilt metadata file.
        assertThrows(IllegalArgumentException.class,
                () -> ModMetadata.quiltModJson(t.mod(), new PlatformTable.Platform("26.1", Loader.QUILT,
                        McVersion.parse("26.1"), "0.31.0", "0.145.1+26.1", 25, null, true, false, null),
                        "0.1.0", List.of(), false, Map.of()));
    }

    @Test
    void parchmentIsPerMinecraftVersionAndOptional() {
        PlatformTable t = PlatformTable.parse(TABLE + """

                [parchment]
                "1.21.1" = "2024.11.17"
                """);
        PlatformTable.Platform p = t.platform("fabric-21.1").orElseThrow();
        assertEquals("2024.11.17", p.parchment());
        assertEquals("org.parchmentmc.data:parchment-1.21.1:2024.11.17@zip", p.parchmentArtifact());
        // Only where a release exists: 26.x has none and keeps the official mappings alone.
        PlatformTable.Platform plain = t.platform("fabric-26.1").orElseThrow();
        assertNull(plain.parchment());
        assertNull(plain.parchmentArtifact());
        // A section key that no platform builds is a typo, not a silent no-op.
        assertThrows(IllegalArgumentException.class, () -> PlatformTable.parse(TABLE + """

                [parchment]
                "1.20.1" = "2024.11.17"
                """), "no platform of that Minecraft version");
    }

    @Test
    void entrypointsFromSources(@TempDir Path dir) throws IOException {
        Path pkg = dir.resolve("lv/example/entrypoint");
        Files.createDirectories(pkg.resolve("client"));
        Files.writeString(pkg.resolve("Common.java"),
                "package lv.example.entrypoint;\npublic final class Common implements ModInitializer {}");
        Files.writeString(pkg.resolve("Second.java"),
                "package lv.example.entrypoint;\npublic final class Second\n        implements net.fabricmc.api.ModInitializer {}");
        Files.writeString(pkg.resolve("Server.java"),
                "package lv.example.entrypoint;\npublic final class Server implements DedicatedServerModInitializer {}");
        Files.writeString(pkg.resolve("client/Client.java"),
                "package lv.example.entrypoint.client;\npublic final class Client implements ClientModInitializer {}");
        Files.writeString(pkg.resolve("Launch.java"),
                "package lv.example.entrypoint;\npublic final class Launch implements PreLaunchEntrypoint {}");
        Files.writeString(pkg.resolve("NotAnEntrypoint.java"),
                "package lv.example.entrypoint;\npublic final class NotAnEntrypoint extends Common {}");
        Files.writeString(pkg.resolve("NotAClass.java"),
                "package lv.example.entrypoint;\npublic interface NotAClass extends ModInitializer {}");
        Files.writeString(dir.resolve("package-info.java"), "package lv.example;");

        Map<EntryPoint, List<String>> roles = ModMetadata.findEntrypoints(dir);
        assertEquals(List.of("lv.example.entrypoint.Common", "lv.example.entrypoint.Second"),
                roles.get(EntryPoint.COMMON));
        assertEquals(List.of("lv.example.entrypoint.client.Client"), roles.get(EntryPoint.CLIENT));
        assertEquals(List.of("lv.example.entrypoint.Server"), roles.get(EntryPoint.SERVER));
        assertEquals(List.of("lv.example.entrypoint.Launch"), roles.get(EntryPoint.LAUNCH));

        Map<String, List<String>> fabric = ModMetadata.loaderEntrypoints(roles, Loader.FABRIC);
        assertEquals(List.of("lv.example.entrypoint.Common", "lv.example.entrypoint.Second"),
                fabric.get(EntryPoint.COMMON.key(Loader.FABRIC)));
        assertEquals(List.of("lv.example.entrypoint.client.Client"),
                fabric.get(EntryPoint.CLIENT.key(Loader.FABRIC)));
        assertEquals(List.of("lv.example.entrypoint.Server"), fabric.get(EntryPoint.SERVER.key(Loader.FABRIC)));
        assertEquals(List.of("lv.example.entrypoint.Launch"), fabric.get(EntryPoint.LAUNCH.key(Loader.FABRIC)));
        assertEquals(4, fabric.size());

        Map<String, List<String>> quilt = ModMetadata.loaderEntrypoints(roles, Loader.QUILT);
        assertEquals(List.of("lv.example.entrypoint.Launch"), quilt.get("pre_launch"));
        assertEquals(List.of("lv.example.entrypoint.Common", "lv.example.entrypoint.Second"), quilt.get("main"));

        assertEquals(Map.of(), ModMetadata.findEntrypoints(dir.resolve("missing")));
    }

    @Test
    void mixinConfigFromSources(@TempDir Path dir) throws IOException {
        Path pkg = dir.resolve("lv/example/mixin");
        Files.createDirectories(pkg.resolve("client"));
        Files.writeString(pkg.resolve("LevelMixin.java"), "package x;\n@Mixin(Level.class)\nclass LevelMixin {}");
        Files.writeString(pkg.resolve("Helper.java"), "package x;\n// mentions @Mixin(Foo) in a comment only\nclass Helper {}");
        Files.writeString(pkg.resolve("client/CameraMixin.java"), "package x;\n  @Mixin(value = Camera.class)\nclass C {}");

        List<String> mixins = ModMetadata.findMixins(dir, "lv.example.mixin");
        assertEquals(List.of("LevelMixin", "client.CameraMixin"), mixins);

        PlatformTable t = PlatformTable.parse(TABLE);
        String cfg = ModMetadata.mixinConfig(t.mod(), t.platform("fabric-21.1").orElseThrow(), mixins);
        assertTrue(cfg.contains("\"mixins\": [\n    \"LevelMixin\"\n  ]"), cfg);
        assertTrue(cfg.contains("\"client\": [\n    \"client.CameraMixin\"\n  ]"), cfg);
        assertTrue(cfg.contains("\"package\": \"lv.example.mixin\""), cfg);
    }
}
