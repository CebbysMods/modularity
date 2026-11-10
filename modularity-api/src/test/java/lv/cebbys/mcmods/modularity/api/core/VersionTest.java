package lv.cebbys.mcmods.modularity.api.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionTest {

    @Test
    void minecraftParsesClassicVersions() {
        MinecraftVersion version = MinecraftVersion.parse("1.21.1");
        assertEquals(1, version.major());
        assertEquals(21, version.minor());
        assertEquals(1, version.patch());
        assertEquals("1.21.1", version.toString());
        assertEquals("1.21.1", version.raw());
    }

    @Test
    void minecraftParsesYearBasedVersions() {
        MinecraftVersion version = MinecraftVersion.parse("26.1.2");
        assertEquals(26, version.major());
        assertEquals(1, version.minor());
        assertEquals(2, version.patch());
        assertTrue(version.compareTo(MinecraftVersion.parse("1.21.11")) > 0);
    }

    @Test
    void minecraftMissingPatchIsZero() {
        MinecraftVersion version = MinecraftVersion.parse(" 1.21 ");
        assertEquals(0, version.patch());
        assertEquals("1.21", version.raw());
        assertEquals(MinecraftVersion.parse("1.21.0"), version);
        assertTrue(version.isAtLeast(1, 21, 0));
        assertFalse(version.isAtLeast(1, 21, 1));
    }

    @Test
    void minecraftRejectsNonReleaseVersions() {
        for (String raw : List.of("24w14a", "1.21-pre1", "1.21.1-rc1", "1", "1.21.1.1", "", "+1.21")) {
            assertThrows(IllegalArgumentException.class, () -> MinecraftVersion.parse(raw), raw);
        }
    }

    @Test
    void platformParsesNumericVersions() {
        PlatformVersion version = PlatformVersion.parse("21.1.265");
        assertEquals(21, version.major());
        assertEquals(1, version.minor());
        assertEquals(265, version.patch());
        assertEquals("21.1.265", version.toString());
        assertTrue(version.isKnown());
    }

    @Test
    void platformKeepsSuffixesInRaw() {
        PlatformVersion version = PlatformVersion.parse("0.20.1+build.42");
        assertEquals(0, version.major());
        assertEquals(20, version.minor());
        assertEquals(1, version.patch());
        assertEquals("0.20.1+build.42", version.toString());
        assertTrue(version.isKnown());
    }

    @Test
    void platformAcceptsUnknownWithoutThrowing() {
        PlatformVersion version = PlatformVersion.parse("unknown");
        assertFalse(version.isKnown());
        assertEquals(PlatformVersion.unknown(), version);
        assertEquals("unknown", version.toString());
        assertEquals(0, version.major());
        assertEquals(PlatformVersion.unknown(), PlatformVersion.parse("  "));
    }

    @Test
    void kindsNeverCompareEqual() {
        MinecraftVersion minecraft = MinecraftVersion.parse("1.21.1");
        PlatformVersion platform = PlatformVersion.parse("1.21.1");
        assertNotEquals(minecraft, platform);
        assertNotEquals(platform, minecraft);
        assertThrows(IllegalArgumentException.class, () -> minecraft.compareTo(platform));
        assertThrows(IllegalArgumentException.class, () -> platform.compareTo(minecraft));
    }
}
