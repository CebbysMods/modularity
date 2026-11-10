package lv.cebbys.mcmods.modularity.api;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Epics;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Features;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Issues;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Stories;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Tags;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Epic(Epics.MODULARITY)
@Feature(Features.L1_UNIT)
@Story(Stories.TESTKIT_CORE)
@Issue(Issues.PR_9)
@Tag(Tags.PR_9)
class CoreApiTest {

    @Test
    void idParsesLikeVanilla() {
        assertEquals(Id.minecraft("stone"), Id.parse("stone"));
        assertEquals(Id.of("refabslab", "double_slab"), Id.parse("refabslab:double_slab"));
        assertEquals("refabslab:block/double_slab", Id.parse("refabslab:double_slab").withPrefix("block/").toString());
        assertThrows(IllegalArgumentException.class, () -> Id.parse("Bad:Path"));
    }

    @Test
    void versionsCompareNumerically() {
        assertTrue(MinecraftVersion.parse("1.21.1").compareTo(MinecraftVersion.parse("1.21")) > 0);
        assertTrue(MinecraftVersion.parse("1.9").compareTo(MinecraftVersion.parse("1.10")) < 0);
        assertEquals(MinecraftVersion.parse("1.20"), MinecraftVersion.parse("1.20.0"));
        assertTrue(MinecraftVersion.parse("1.20.6").isAtLeast("1.20.5"));
        assertThrows(IllegalArgumentException.class, () -> MinecraftVersion.parse("24w10a"));
    }

    @Test
    void rangesAreHalfOpen() {
        VersionRange range = VersionRange.between("1.20", "1.21");
        assertTrue(range.contains(MinecraftVersion.parse("1.20")));
        assertTrue(range.contains(MinecraftVersion.parse("1.20.6")));
        assertFalse(range.contains(MinecraftVersion.parse("1.21")));
        assertThrows(IllegalArgumentException.class, () -> VersionRange.between("1.21", "1.20"));
    }

    @Test
    void versionedSelectsNewestCompatibleImplementation() {
        Versioned<String> impl = Versioned.<String>builder()
                .since("1.21.2", () -> "new")
                .since("1.20", () -> "mid")
                .fallback(() -> "legacy")
                .build();
        assertEquals("new", impl.select(MinecraftVersion.parse("1.21.4")));
        assertEquals("mid", impl.select(MinecraftVersion.parse("1.21.1")));
        assertEquals("legacy", impl.select(MinecraftVersion.parse("1.18.2")));
        assertThrows(IllegalStateException.class,
                () -> Versioned.<String>builder().since("1.21", () -> "x").select(MinecraftVersion.parse("1.19")));
    }

    @Test
    void blockDefinitionValidatesPropertyDefaults() {
        IntegerProperty light = Property.integer("light_level", 0, 15);
        BlockDefinition definition = BlockDefinition.builder().property(light, 3).noLootTable().build();
        assertEquals(3, definition.properties().get(light));
        assertFalse(definition.hasLootTable());
        assertThrows(IllegalArgumentException.class, () -> BlockDefinition.builder().property(light, 16));
    }

    @Test
    void codecsRoundTrip() {
        record Pair(Id a, int b) {
        }
        DataCodec<Pair> codec = DataCodec.of(
                (pair, out) -> out.putId("a", pair.a()).putInt("b", pair.b()),
                in -> new Pair(in.getId("a"), in.getInt("b")));
        MapData data = new MapData();
        codec.write(new Pair(Id.minecraft("oak_slab"), 7), data);
        assertEquals(new Pair(Id.minecraft("oak_slab"), 7), codec.read(data));
    }
}
