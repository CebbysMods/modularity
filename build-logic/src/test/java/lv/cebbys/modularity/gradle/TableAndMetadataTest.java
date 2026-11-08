package lv.cebbys.modularity.gradle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
            name = "Modularity"
            description = "API."
            license = "MIT"
            authors = ["CebbyS"]
            mixin-package = "lv.example.mixin"
            mods = [":a", ":b"]

            [mod.entrypoints]
            main = ["lv.example.Main"]

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
        assertFalse(p.enabled());
        assertFalse(t.platform("fabric-26.1").orElseThrow().remap());
        assertEquals(List.of("fabric-26.1"), t.enabled().stream().map(PlatformTable.Platform::id).toList());
        assertEquals(List.of(":a", ":b"), t.mod().mods());
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
        assertEquals("platforms/neoforge/versions/21.1", neo.projectDir());
        assertEquals(List.of("minecraft", "neoforge"), neo.loader().layerRoots());
        PlatformTable.Platform quilt = t.platform("quilt-21.1").orElseThrow();
        assertTrue(quilt.remap());
        assertFalse(quilt.loader().publishes(), "Quilt runs the Fabric jar");
        assertEquals(List.of("minecraft", "fabric", "quilt"), quilt.loader().layerRoots());
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
        assertTrue(ModMetadata.fabricModJson(withIcon.mod(), t.platform("fabric-21.1").orElseThrow(), "0.16.0", "0.1.0", false)
                .contains("\"icon\": \"assets/modularity/icon.png\""));

        assertEquals("[0,)", ModMetadata.mavenRange("*"));
        assertEquals("[1.2.3]", ModMetadata.mavenRange("1.2.3"));
        assertThrows(IllegalArgumentException.class, () -> ModMetadata.mavenRange("~1.2"));
        assertEquals(":utilities:mvd:mvd-neoforge", NeoForgeSetup.loaderModule(":utilities:mvd:mvd-fabric", Loader.NEOFORGE));
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
        String json = ModMetadata.fabricModJson(t.mod(), t.platform("fabric-21.1").orElseThrow(), t.fabricLoaderMin(), "0.1.0", true);
        assertTrue(json.contains("\"minecraft\": \"1.21.1\""), json);
        assertTrue(json.contains("\"mvd\": \">=4.0.0\""), json);
        assertTrue(json.contains("\"fabricloader\": \">=0.16.0\""), json);
        assertTrue(json.contains("\"java\": \">=21\""), json);
        assertTrue(json.contains("\"mixins\": [\n    \"modularity.mixins.json\"\n  ]"), json);
        assertTrue(json.contains("\"description\": \"API. Fabric 1.21.1 platform.\""), json);
        assertFalse(ModMetadata.fabricModJson(t.mod(), t.platform("fabric-26.1").orElseThrow(), t.fabricLoaderMin(), "0.1.0", false)
                .contains("mixins"));
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
