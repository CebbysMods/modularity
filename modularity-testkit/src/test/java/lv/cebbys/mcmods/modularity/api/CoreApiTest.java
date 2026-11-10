package lv.cebbys.mcmods.modularity.api;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import lv.cebbys.mcmods.modularity.api.block.BlockDefinition;
import lv.cebbys.mcmods.modularity.api.block.property.IntegerProperty;
import lv.cebbys.mcmods.modularity.api.block.property.Property;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.core.MinecraftVersion;
import lv.cebbys.mcmods.modularity.api.core.VersionRange;
import lv.cebbys.mcmods.modularity.api.core.Versioned;
import lv.cebbys.mcmods.modularity.api.data.DataCodec;
import lv.cebbys.mcmods.modularity.api.data.MapData;
import lv.cebbys.mcmods.modularity.testing.allure.Functionality;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Epics;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Features;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Functionalities;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Issues;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Stories;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Tags;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ids, ranges, versioned code, block definitions and data codecs. */
@Epic(Epics.MODULARITY)
@Feature(Features.L1_UNIT)
@Story(Stories.CORE_API)
@Issue(Issues.PR_4)
@Tag(Tags.PR_4)
class CoreApiTest {

    @Test
    @Functionality(Functionalities.NAMESPACED_IDS)
    @DisplayName("A namespaced id parses like a vanilla resource location")
    void idParsesLikeVanilla() {
        assertParsesLike("stone", Id.minecraft("stone"));
        assertParsesLike("refabslab:double_slab", Id.of("refabslab", "double_slab"));
        assertPrefix("refabslab:double_slab", "block/", "refabslab:block/double_slab");
        assertIdRejected("Bad:Path");
    }

    @Test
    @Functionality(Functionalities.VERSION_COMPARISON)
    @DisplayName("Minecraft versions compare by number, not by text")
    void versionsCompareNumerically() {
        assertGreater("1.21.1", "1.21");
        assertLess("1.9", "1.10");
        assertSameVersion("1.20", "1.20.0");
        assertAtLeast("1.20.6", "1.20.5");
        assertVersionRejected("24w10a");
    }

    @Test
    @Functionality(Functionalities.VERSION_RANGES)
    @DisplayName("A version range includes its lower bound and excludes its upper bound")
    void rangesAreHalfOpen() {
        VersionRange range = range("1.20", "1.21");
        assertInRange(range, "1.20", true);
        assertInRange(range, "1.20.6", true);
        assertInRange(range, "1.21", false);
        assertRangeRejected("1.21", "1.20");
    }

    @Test
    @Functionality(Functionalities.VERSIONED_SELECTION)
    @DisplayName("Versioned code selects the newest implementation the game version supports")
    void versionedSelectsNewestCompatibleImplementation() {
        Versioned<String> implementation = buildVersioned();
        assertSelected(implementation, "1.21.4", "new");
        assertSelected(implementation, "1.21.1", "mid");
        assertSelected(implementation, "1.18.2", "legacy");
        assertNoImplementation("1.19");
    }

    @Test
    @Functionality(Functionalities.BLOCK_DEFINITIONS)
    @DisplayName("A block definition rejects a default outside the property range")
    void blockDefinitionValidatesPropertyDefaults() {
        IntegerProperty light = lightProperty();
        BlockDefinition definition = blockDefinition(light, 3);
        assertDefinition(light, definition);
        assertDefaultRejected(light, 16);
    }

    @Test
    @Functionality(Functionalities.DATA_CODECS)
    @DisplayName("A data codec writes a value and reads it back")
    void codecsRoundTrip() {
        DataCodec<Pair> codec = pairCodec();
        MapData data = new MapData();
        writePair(codec, data);
        assertReadPair(codec, data);
    }

    /** The value the codec test writes and reads. */
    record Pair(Id a, int b) {
    }

    @Step("Parse \"{0}\" and compare it with the expected id")
    private void assertParsesLike(String text, Id expected) {
        assertEquals(expected, Id.parse(text));
    }

    @Step("Add the prefix \"{1}\" to the id \"{0}\"")
    private void assertPrefix(String id, String prefix, String expected) {
        assertEquals(expected, Id.parse(id).withPrefix(prefix).toString());
    }

    @Step("Reject \"{0}\" because the namespace has capitals")
    private void assertIdRejected(String text) {
        assertThrows(IllegalArgumentException.class, () -> Id.parse(text), "\"" + text + "\" is not a valid id");
    }

    @Step("Check that \"{0}\" is above \"{1}\"")
    private void assertGreater(String bigger, String smaller) {
        assertTrue(MinecraftVersion.parse(bigger).compareTo(MinecraftVersion.parse(smaller)) > 0,
                bigger + " is above " + smaller);
    }

    @Step("Check that \"{0}\" is below \"{1}\"")
    private void assertLess(String smaller, String bigger) {
        assertTrue(MinecraftVersion.parse(smaller).compareTo(MinecraftVersion.parse(bigger)) < 0,
                smaller + " is below " + bigger);
    }

    @Step("Check that \"{0}\" and \"{1}\" are the same version")
    private void assertSameVersion(String left, String right) {
        assertEquals(MinecraftVersion.parse(left), MinecraftVersion.parse(right));
    }

    @Step("Check that \"{0}\" is at least \"{1}\"")
    private void assertAtLeast(String version, String minimum) {
        assertTrue(MinecraftVersion.parse(version).isAtLeast(minimum), version + " is at least " + minimum);
    }

    @Step("Reject \"{0}\" as a Minecraft version")
    private void assertVersionRejected(String text) {
        assertThrows(IllegalArgumentException.class, () -> MinecraftVersion.parse(text));
    }

    @Step("Build the version range from \"{0}\" to \"{1}\"")
    private VersionRange range(String from, String to) {
        return VersionRange.between(from, to);
    }

    @Step("Check that the range contains \"{1}\": {2}")
    private void assertInRange(VersionRange range, String version, boolean inside) {
        assertEquals(inside, range.contains(MinecraftVersion.parse(version)),
                version + " is inside the range: " + inside);
    }

    @Step("Reject the reversed range from \"{0}\" to \"{1}\"")
    private void assertRangeRejected(String from, String to) {
        assertThrows(IllegalArgumentException.class, () -> VersionRange.between(from, to));
    }

    @Step("Build versioned code with an implementation since 1.21.2, since 1.20 and a fallback")
    private Versioned<String> buildVersioned() {
        return Versioned.<String>builder()
                .since("1.21.2", () -> "new")
                .since("1.20", () -> "mid")
                .fallback(() -> "legacy")
                .build();
    }

    @Step("Select the implementation for Minecraft \"{1}\" and expect \"{2}\"")
    private void assertSelected(Versioned<String> implementation, String version, String expected) {
        assertEquals(expected, implementation.select(MinecraftVersion.parse(version)));
    }

    @Step("Fail when no implementation matches Minecraft \"{0}\"")
    private void assertNoImplementation(String version) {
        assertThrows(IllegalStateException.class,
                () -> Versioned.<String>builder().since("1.21", () -> "x").select(MinecraftVersion.parse(version)));
    }

    @Step("Create an integer property light_level from 0 to 15")
    private IntegerProperty lightProperty() {
        return Property.integer("light_level", 0, 15);
    }

    @Step("Build a block definition with the light default {1} and no loot table")
    private BlockDefinition blockDefinition(IntegerProperty light, int defaultValue) {
        return BlockDefinition.builder().property(light, defaultValue).noLootTable().build();
    }

    @Step("Check the default value 3 and the missing loot table")
    private void assertDefinition(IntegerProperty light, BlockDefinition definition) {
        assertEquals(3, definition.properties().get(light), "the default value is 3");
        assertFalse(definition.hasLootTable(), "the definition has no loot table");
    }

    @Step("Reject the light default {1} because it is outside 0 to 15")
    private void assertDefaultRejected(IntegerProperty light, int defaultValue) {
        assertThrows(IllegalArgumentException.class, () -> BlockDefinition.builder().property(light, defaultValue));
    }

    @Step("Define a data codec for a pair of an id and a number")
    private DataCodec<Pair> pairCodec() {
        return DataCodec.of(
                (pair, out) -> out.putId("a", pair.a()).putInt("b", pair.b()),
                in -> new Pair(in.getId("a"), in.getInt("b")));
    }

    @Step("Write the pair oak_slab and 7 into the data map")
    private void writePair(DataCodec<Pair> codec, MapData data) {
        codec.write(new Pair(Id.minecraft("oak_slab"), 7), data);
    }

    @Step("Read the pair back and compare it with the written value")
    private void assertReadPair(DataCodec<Pair> codec, MapData data) {
        assertEquals(new Pair(Id.minecraft("oak_slab"), 7), codec.read(data));
    }
}
