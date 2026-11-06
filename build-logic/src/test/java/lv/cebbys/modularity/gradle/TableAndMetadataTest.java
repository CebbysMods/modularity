package lv.cebbys.modularity.gradle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertEquals(":platforms:fabric-21.1", p.projectPath());
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
