package lv.cebbys.modularity.gradle.smoke;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmokeCheckTest {

    private static final String SERVER_LOG = """
            [17:39:33] [main/INFO] (modularity) (server) platform fabric:0.20.1, minecraft:1.21.10
            [17:39:33] [main/INFO] (modularity-smoke) Mod modularity: loaded=true version=0.1.0-SNAPSHOT nested=no
            [17:39:33] [main/INFO] (modularity) Loaded guest mod smoke_guest 1.0.0 from guest.jar
            [17:39:34] [Server thread/INFO] (modularity-smoke) Guest mod smoke_guest: loaded=true initialized=true
            [17:39:35] [Server thread/INFO] (modularity-smoke) Server ticked 40 times, stopping
            """;

    @Test
    void serverLogPasses() {
        SmokeCheck.Result r = SmokeCheck.check(SERVER_LOG, "1.21.10", SmokeCheck.Side.SERVER);
        assertTrue(r.ok(), r.problems().toString());
        assertEquals(5, r.lines().size());
    }

    @Test
    void guestNotInitializedFails() {
        SmokeCheck.Result r = SmokeCheck.check(SERVER_LOG.replace("initialized=true", "initialized=false"), "1.21.10",
                SmokeCheck.Side.SERVER);
        assertFalse(r.ok());
    }

    @Test
    void wrongVersionFails() {
        SmokeCheck.Result r = SmokeCheck.check(SERVER_LOG, "1.21.1", SmokeCheck.Side.SERVER);
        assertFalse(r.ok());
        assertTrue(r.problems().stream().anyMatch(p -> p.startsWith("detection reports a different version")),
                r.problems().toString());
    }

    @Test
    void nestedModFails() {
        SmokeCheck.Result r = SmokeCheck.check(SERVER_LOG.replace(
                        "Mod modularity: loaded=true version=0.1.0-SNAPSHOT nested=no",
                        "Mod modularity: loaded=true version=0.1.0-SNAPSHOT nested=modularity"),
                "1.21.10", SmokeCheck.Side.SERVER);
        assertFalse(r.ok());
    }

    @Test
    void clientNeedsClientSideAndScreenshot() {
        SmokeCheck.Result r = SmokeCheck.check(SERVER_LOG, "1.21.10", SmokeCheck.Side.CLIENT);
        assertFalse(r.ok());
        String client = SERVER_LOG.replace("(server)", "(client)")
                + "[x] Client joined world\n[x] Screenshot: Saved screenshot as smoke.png\n[x] Client done\n";
        assertTrue(SmokeCheck.check(client, "1.21.10", SmokeCheck.Side.CLIENT).ok());
    }

    @Test
    void quiltServerPassesWithoutGuests() {
        SmokeCheck.Result r = SmokeCheck.check(SERVER_LOG.replace("platform fabric:0.20.1", "platform quilt:0.30.2"),
                "1.21.10", SmokeCheck.Side.SERVER, "QUILT", false);
        assertTrue(r.ok(), r.problems().toString());
    }

    @Test
    void crashFails() {
        assertFalse(SmokeCheck.check(SERVER_LOG + "---- Minecraft Crash Report ----\n", "1.21.10", SmokeCheck.Side.SERVER).ok());
    }
}
