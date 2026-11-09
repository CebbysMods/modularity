package lv.cebbys.modularity.gradle.smoke;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmokeCheckTest {

    private static final String SERVER_LOG = """
            [17:39:33] [main/INFO] (mvd) [mvd] Detected version 1.21.10 (source: fabric-loader)
            [17:39:33] [main/INFO] (modularity-smoke) [smoke] mod modularity: loaded=true version=0.1.0-SNAPSHOT nested=no
            [17:39:33] [main/INFO] (modularity-smoke) [smoke] mod mvd: loaded=true version=4.0.0 nested=no
            [17:39:33] [main/INFO] (modularity-smoke) [smoke] mod mpd: loaded=true version=1.0.0 nested=no
            [17:39:33] [main/INFO] (mpd) [mpd] Detected platform FABRIC 0.20.1, SERVER (development) (source: fabric-loader)
            [17:39:33] [main/INFO] (modularity) [modularity] Loaded guest mod smoke_guest 1.0.0 from guest.jar
            [17:39:34] [Server thread/INFO] (modularity-smoke) [smoke] guest mod smoke_guest: loaded=true initialized=true
            [17:39:35] [Server thread/INFO] (modularity-smoke) [smoke] server ticked 40 times, stopping
            """;

    @Test
    void serverLogPasses() {
        SmokeCheck.Result r = SmokeCheck.check(SERVER_LOG, "1.21.10", SmokeCheck.Side.SERVER);
        assertTrue(r.ok(), r.problems().toString());
        assertEquals(8, r.lines().size());
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
        assertTrue(r.problems().stream().anyMatch(p -> p.startsWith("mvd reports a different version")), r.problems().toString());
    }

    @Test
    void nestedModFails() {
        SmokeCheck.Result r = SmokeCheck.check(SERVER_LOG.replace("mvd: loaded=true version=4.0.0 nested=no",
                "mvd: loaded=true version=4.0.0 nested=modularity"), "1.21.10", SmokeCheck.Side.SERVER);
        assertFalse(r.ok());
    }

    @Test
    void clientNeedsClientSideAndScreenshot() {
        SmokeCheck.Result r = SmokeCheck.check(SERVER_LOG, "1.21.10", SmokeCheck.Side.CLIENT);
        assertFalse(r.ok());
        String client = SERVER_LOG.replace("SERVER (development)", "CLIENT (development)")
                + "[x] [smoke] client joined world\n[x] [smoke] screenshot: Saved screenshot as smoke.png\n[x] [smoke] client done\n";
        assertTrue(SmokeCheck.check(client, "1.21.10", SmokeCheck.Side.CLIENT).ok());
    }

    @Test
    void quiltServerPassesWithoutGuests() {
        SmokeCheck.Result r = SmokeCheck.check(SERVER_LOG.replace("platform FABRIC 0.20.1", "platform QUILT 0.30.2"),
                "1.21.10", SmokeCheck.Side.SERVER, "QUILT", false);
        assertTrue(r.ok(), r.problems().toString());
    }

    @Test
    void crashFails() {
        assertFalse(SmokeCheck.check(SERVER_LOG + "---- Minecraft Crash Report ----\n", "1.21.10", SmokeCheck.Side.SERVER).ok());
    }
}
