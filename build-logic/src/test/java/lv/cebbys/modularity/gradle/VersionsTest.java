package lv.cebbys.modularity.gradle;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionsTest {

    private static McVersion v(String s) {
        return McVersion.parse(s);
    }

    @Test
    void versionsCompareNumerically() {
        assertTrue(v("1.21.10").compareTo(v("1.21.9")) > 0);
        assertTrue(v("1.21.11").compareTo(v("26.1")) < 0);
        assertTrue(v("26.1").compareTo(v("26.1.1")) < 0);
        assertEquals(v("26.1"), v("26.1.0"));
        assertEquals(v("26.1").hashCode(), v("26.1.0").hashCode());
    }

    @Test
    void shortNames() {
        assertEquals("21.1", v("1.21.1").shortName());
        assertEquals("26.1.2", v("26.1.2").shortName());
        assertEquals(v("1.21.10"), McVersion.parseShort("21.10"));
        assertEquals(v("26.3"), McVersion.parseShort("26.3"));
    }

    @Test
    void obfuscatedUpTo12111() {
        assertTrue(v("1.21.11").obfuscated());
        assertFalse(v("26.1").obfuscated());
    }

    @Test
    void badVersionsFail() {
        for (String s : List.of("", "1", "1.21.x", "1..2", "26.1-pre1", "a.b")) {
            assertThrows(IllegalArgumentException.class, () -> McVersion.parse(s), s);
        }
    }

    @Test
    void openRange() {
        VersionRange r = VersionRange.parse("1.21.5+");
        assertFalse(r.contains(v("1.21.4")));
        assertTrue(r.contains(v("1.21.5")));
        assertTrue(r.contains(v("26.3")));
        assertEquals("1.21.5+", r.toString());
    }

    @Test
    void closedRangeIsInclusive() {
        VersionRange r = VersionRange.parse("1.21.5-1.21.8");
        assertFalse(r.contains(v("1.21.4")));
        assertTrue(r.contains(v("1.21.5")));
        assertTrue(r.contains(v("1.21.8")));
        assertFalse(r.contains(v("1.21.9")));
    }

    @Test
    void unparsableRangesFail() {
        for (String s : List.of("1.21.5", "1.21.5++", "+", "1.21.8-1.21.5", "1.21.5-1.21.6-1.21.7", "latest",
                "1.21.x+", "-1.21.5")) {
            assertThrows(IllegalArgumentException.class, () -> VersionRange.parse(s), s);
        }
    }

    @Test
    void rangesOrderByStartThenEndOpenLast() {
        List<VersionRange> list = new ArrayList<>(List.of(
                VersionRange.parse("1.21.5+"), VersionRange.parse("26.1+"), VersionRange.parse("1.21.5-1.21.8"),
                VersionRange.parse("1.21.2+"), VersionRange.parse("1.21.5-1.21.6")));
        list.sort(null);
        assertEquals("[1.21.2+, 1.21.5-1.21.6, 1.21.5-1.21.8, 1.21.5+, 26.1+]", list.toString());
    }
}
