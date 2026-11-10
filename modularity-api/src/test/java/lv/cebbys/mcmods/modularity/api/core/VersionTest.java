package lv.cebbys.mcmods.modularity.api.core;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import lv.cebbys.mcmods.modularity.testing.allure.Functionality;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Epics;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Features;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Functionalities;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Issues;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Stories;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Tags;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Minecraft and platform version values: parsing, text form and comparison. */
@Epic(Epics.MODULARITY)
@Feature(Features.L1_UNIT)
@Story(Stories.VERSION_API)
@Issue(Issues.PR_4)
@Tag(Tags.PR_4)
class VersionTest {

    @Test
    @Functionality(Functionalities.MINECRAFT_PARSING)
    @DisplayName("A classic Minecraft version parses into major, minor and patch")
    void minecraftParsesClassicVersions() {
        MinecraftVersion version = parseMinecraft("1.21.1");
        assertNumbers(version, 1, 21, 1);
        assertText(version, "1.21.1");
    }

    @Test
    @Functionality(Functionalities.MINECRAFT_PARSING)
    @DisplayName("A year-based Minecraft version parses and compares above the old scheme")
    void minecraftParsesYearBasedVersions() {
        MinecraftVersion version = parseMinecraft("26.1.2");
        assertNumbers(version, 26, 1, 2);
        compareAbove("26.1.2", "1.21.11");
    }

    @Test
    @Functionality(Functionalities.MINECRAFT_PARSING)
    @DisplayName("A Minecraft version without a patch number gets patch 0")
    void minecraftMissingPatchIsZero() {
        MinecraftVersion version = parseMinecraft(" 1.21 ");
        assertMissingPatch(version, "1.21");
        assertSameVersion(version, "1.21.0");
        assertAtLeast(version, "1.21.0", true);
        assertAtLeast(version, "1.21.1", false);
    }

    @Test
    @Functionality(Functionalities.MINECRAFT_PARSING)
    @DisplayName("Snapshots, pre-releases and malformed versions are rejected")
    void minecraftRejectsNonReleaseVersions() {
        for (String raw : List.of("24w14a", "1.21-pre1", "1.21.1-rc1", "1", "1.21.1.1", "", "+1.21")) {
            assertRejected(raw);
        }
    }

    @Test
    @Functionality(Functionalities.PLATFORM_PARSING)
    @DisplayName("A three-part platform version parses into major, minor and patch")
    void platformParsesNumericVersions() {
        PlatformVersion version = parsePlatform("21.1.265");
        assertNumbers(version, 21, 1, 265);
        assertText(version, "21.1.265");
        assertKnown(version, true);
    }

    @Test
    @Functionality(Functionalities.PLATFORM_PARSING)
    @DisplayName("A platform version keeps its build suffix in the raw text")
    void platformKeepsSuffixesInRaw() {
        PlatformVersion version = parsePlatform("0.20.1+build.42");
        assertNumbers(version, 0, 20, 1);
        assertText(version, "0.20.1+build.42");
        assertKnown(version, true);
    }

    @Test
    @Functionality(Functionalities.PLATFORM_PARSING)
    @DisplayName("An unknown platform version is reported as unknown, not as an error")
    void platformAcceptsUnknownWithoutThrowing() {
        PlatformVersion version = parsePlatform("unknown");
        assertKnown(version, false);
        assertText(version, "unknown");
        assertNumbers(version, 0, 0, 0);
        assertBlankIsUnknown();
    }

    @Test
    @Functionality(Functionalities.VERSION_COMPARISON)
    @DisplayName("A Minecraft version never equals a platform version")
    void kindsNeverCompareEqual() {
        MinecraftVersion minecraft = parseMinecraft("1.21.1");
        PlatformVersion platform = parsePlatform("1.21.1");
        assertDifferentKinds(minecraft, platform);
    }

    @Step("Parse \"{0}\" as a Minecraft version")
    private MinecraftVersion parseMinecraft(String raw) {
        return MinecraftVersion.parse(raw);
    }

    @Step("Parse \"{0}\" as a platform version")
    private PlatformVersion parsePlatform(String raw) {
        return PlatformVersion.parse(raw);
    }

    @Step("Check that the major, minor and patch numbers are {0}.{1}.{2}")
    private void assertNumbers(Version version, int major, int minor, int patch) {
        assertEquals(major, version.major(), "major");
        assertEquals(minor, version.minor(), "minor");
        assertEquals(patch, version.patch(), "patch");
    }

    @Step("Check that the text form and the raw text are \"{1}\"")
    private void assertText(Version version, String text) {
        assertEquals(text, version.toString(), "text form");
        assertEquals(text, version.raw(), "raw text");
    }

    @Step("Check that the version is known: {1}")
    private void assertKnown(PlatformVersion version, boolean known) {
        assertEquals(known, version.isKnown());
        if (!known) {
            assertEquals(PlatformVersion.unknown(), version, "the value is the unknown constant");
        }
    }

    @Step("Check that the missing patch is 0 and that the raw text is \"{1}\"")
    private void assertMissingPatch(MinecraftVersion version, String raw) {
        assertEquals(0, version.patch(), "the missing patch is 0");
        assertEquals(raw, version.raw(), "the raw text keeps the input without spaces");
    }

    @Step("Check that the version equals \"{1}\"")
    private void assertSameVersion(MinecraftVersion version, String other) {
        assertEquals(MinecraftVersion.parse(other), version);
    }

    @Step("Check that blank text gives the unknown constant")
    private void assertBlankIsUnknown() {
        assertEquals(PlatformVersion.unknown(), PlatformVersion.parse("  "));
    }

    @Step("Check that a Minecraft version and a platform version are never equal")
    private void assertDifferentKinds(MinecraftVersion minecraft, PlatformVersion platform) {
        assertNotEquals(minecraft, platform, "a Minecraft version is not equal to a platform version");
        assertNotEquals(platform, minecraft, "equality is symmetric");
        assertThrows(IllegalArgumentException.class, () -> minecraft.compareTo(platform),
                "a Minecraft version does not compare with a platform version");
        assertThrows(IllegalArgumentException.class, () -> platform.compareTo(minecraft),
                "a platform version does not compare with a Minecraft version");
    }

    @Step("Compare \"{0}\" with the older version \"{1}\"")
    private void compareAbove(String version, String older) {
        MinecraftVersion newer = MinecraftVersion.parse(version);
        assertTrue(newer.compareTo(MinecraftVersion.parse(older)) > 0, newer + " is above " + older);
    }

    @Step("Check that isAtLeast(\"{1}\") is {2}")
    private void assertAtLeast(MinecraftVersion version, String other, boolean expected) {
        assertEquals(expected, version.isAtLeast(other), version + " is at least " + other);
    }

    @Step("Reject \"{0}\" as a Minecraft version")
    private void assertRejected(String raw) {
        assertThrows(IllegalArgumentException.class, () -> MinecraftVersion.parse(raw),
                "\"" + raw + "\" is not a release version");
    }
}
