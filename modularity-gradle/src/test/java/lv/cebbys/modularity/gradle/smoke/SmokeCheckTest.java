package lv.cebbys.modularity.gradle.smoke;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static lv.cebbys.modularity.gradle.allure.BuildAllureLabels.Epics;
import static lv.cebbys.modularity.gradle.allure.BuildAllureLabels.Features;
import static lv.cebbys.modularity.gradle.allure.BuildAllureLabels.Issues;
import static lv.cebbys.modularity.gradle.allure.BuildAllureLabels.Stories;
import static lv.cebbys.modularity.gradle.allure.BuildAllureLabels.Tags;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The smoke log checks: the lines a run must print, and the failures it must report. */
@Epic(Epics.MODULARITY)
@Feature(Features.UNIT)
@Story(Stories.SMOKE_CHECKS)
@Issue(Issues.PR_11)
@Tag(Tags.PR_11)
class SmokeCheckTest {

    private static final String SERVER_LOG = """
            [17:39:30] [main/INFO] (modularity) Entrypoint launch active (fabric)
            [17:39:31] [main/INFO] (modularity) Loaded guest mod smoke_guest 1.0.0 from guest.jar
            [17:39:32] [main/INFO] (modularity) Entrypoint common active (fabric)
            [17:39:33] [main/INFO] (modularity) (server) platform fabric:0.20.1, minecraft:1.21.10
            [17:39:33] [main/INFO] (modularity) Entrypoint server active (fabric)
            [17:39:33] [main/INFO] (modularity-smoke) Smoke features: declared block=modularity_smoke:feature_block item=modularity_smoke:feature_item channel=modularity_smoke:feature
            [17:39:33] [main/INFO] (modularity-smoke) Smoke features: resolved server block=modularity_smoke:feature_block present=true
            [17:39:33] [main/INFO] (modularity-smoke) Smoke storage: unitsPerMb=81 inserted=1000 units=81000 drained=400 amount=600
            [17:39:33] [main/INFO] (modularity-smoke) Mod modularity: loaded=true version=0.1.0-SNAPSHOT nested=no
            [17:39:34] [Server thread/INFO] (modularity-smoke) Guest mod smoke_guest: loaded=true initialized=true
            [17:39:35] [Server thread/INFO] (modularity-smoke) Server ticked 40 times, stopping
            """;

    /** A Forge server log that meets every Forge expectation: the mod constructor stage first, then the others. */
    private static String forgeServerLog() {
        return SERVER_LOG.replace("(fabric)", "(forge)")
                .replace("platform fabric:0.20.1", "platform forge:52.1.21")
                .replace("unitsPerMb=81 inserted=1000 units=81000", "unitsPerMb=1 inserted=1000 units=1000")
                .replace("Entrypoint launch active (forge)",
                        "Entrypoint mod-constructor active (forge)\n[x] Entrypoint launch active (forge)");
    }

    @Test
    @DisplayName("A complete server log passes")
    void serverLogPasses() {
        SmokeCheck.Result r = SmokeCheck.check(SERVER_LOG, "1.21.10", SmokeCheck.Side.SERVER);
        assertTrue(r.ok(), r.problems().toString());
        assertEquals(11, r.lines().size());
    }

    @Test
    void storageLineMustAddUp() {
        // The line's numbers are the proof of the loader's unit, so a consistent-looking wrong line must fail.
        String wrongUnits = SERVER_LOG.replace("units=81000 drained=400 amount=600", "units=80000 drained=400 amount=600");
        SmokeCheck.Result r = SmokeCheck.check(wrongUnits, "1.21.10", SmokeCheck.Side.SERVER);
        assertFalse(r.ok(), r.problems().toString());
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("units where 1000 mB needs")), r.problems().toString());
        // The portable view must report millibuckets after the drain.
        String wrongAmount = SERVER_LOG.replace("amount=600", "amount=81000");
        assertTrue(SmokeCheck.check(wrongAmount, "1.21.10", SmokeCheck.Side.SERVER).problems().stream()
                .anyMatch(p -> p.contains("after 1000 mB in and 400 mB out")));
        // A loader that keeps no unit of its own cannot convert.
        String noUnit = SERVER_LOG.replace("unitsPerMb=81 inserted=1000 units=81000", "unitsPerMb=0 inserted=0 units=0");
        assertTrue(SmokeCheck.check(noUnit, "1.21.10", SmokeCheck.Side.SERVER).problems().stream()
                .anyMatch(p -> p.contains("unitsPerMb=0")));
    }

    @Test
    void missingStageLineFails() {
        SmokeCheck.Result r = SmokeCheck.check(SERVER_LOG.replace("Entrypoint common active (fabric)\n", ""),
                "1.21.10", SmokeCheck.Side.SERVER);
        assertFalse(r.ok(), r.problems().toString());
    }

    @Test
    void missingFeatureLineFails() {
        SmokeCheck.Result r = SmokeCheck.check(
                SERVER_LOG.replace("Smoke features: resolved server block=modularity_smoke:feature_block present=true\n", ""),
                "1.21.10", SmokeCheck.Side.SERVER);
        assertFalse(r.ok(), r.problems().toString());
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("Smoke features: resolved")), r.problems().toString());
    }

    @Test
    void forgeAlsoNeedsFeatureLines() {
        String forge = forgeServerLog();
        SmokeCheck.Result r = SmokeCheck.check(forge, "1.21.10", SmokeCheck.Side.SERVER, "FORGE", true);
        assertTrue(r.problems().stream().noneMatch(p -> p.contains("Smoke features")), r.problems().toString());
        String withoutFeatures = forge.replace(
                "Smoke features: resolved server block=modularity_smoke:feature_block present=true\n", "");
        assertTrue(SmokeCheck.check(withoutFeatures, "1.21.10", SmokeCheck.Side.SERVER, "FORGE", true).problems()
                .stream().anyMatch(p -> p.contains("Smoke features: resolved")));
    }

    @Test
    void forgeFamilyNeedsModConstructorStage() {
        String forge = forgeServerLog();
        SmokeCheck.Result r = SmokeCheck.check(forge, "1.21.10", SmokeCheck.Side.SERVER, "FORGE", true);
        assertTrue(r.ok(), r.problems().toString());
        String withoutConstructor = forge.replace("Entrypoint mod-constructor active (forge)\n", "");
        assertFalse(SmokeCheck.check(withoutConstructor, "1.21.10", SmokeCheck.Side.SERVER, "FORGE", true).ok());
    }

    @Test
    void forgeServerLogPassesWithoutMixinLine() {
        // A Forge development run cannot apply the mixin config: its manifest attribute reaches Mixin from a shipped
        // jar only (docs/07 7.8). The check must not require the line, or every Forge smoke fails.
        String forge = forgeServerLog()
                + "[17:39:30] [main/INFO] [mixin/]: SpongePowered MIXIN Subsystem Version=0.8.7 Service=ModLauncher\n";
        SmokeCheck.Result r = SmokeCheck.check(forge, "1.21.10", SmokeCheck.Side.SERVER, "FORGE", true);
        assertTrue(r.ok(), r.problems().toString());
    }

    @Test
    @DisplayName("A guest mod that does not initialize fails")
    void guestNotInitializedFails() {
        SmokeCheck.Result r = SmokeCheck.check(SERVER_LOG.replace("initialized=true", "initialized=false"), "1.21.10",
                SmokeCheck.Side.SERVER);
        assertFalse(r.ok());
    }

    @Test
    @DisplayName("A log with another Minecraft version fails")
    void wrongVersionFails() {
        SmokeCheck.Result r = SmokeCheck.check(SERVER_LOG, "1.21.1", SmokeCheck.Side.SERVER);
        assertFalse(r.ok());
        assertTrue(r.problems().stream().anyMatch(p -> p.startsWith("detection reports a different version")),
                r.problems().toString());
    }

    @Test
    @DisplayName("A nested jar load fails")
    void nestedModFails() {
        SmokeCheck.Result r = SmokeCheck.check(SERVER_LOG.replace(
                        "Mod modularity: loaded=true version=0.1.0-SNAPSHOT nested=no",
                        "Mod modularity: loaded=true version=0.1.0-SNAPSHOT nested=modularity"),
                "1.21.10", SmokeCheck.Side.SERVER);
        assertFalse(r.ok());
    }

    @Test
    @DisplayName("A client log needs the client side and a screenshot")
    void clientNeedsClientSideAndScreenshot() {
        SmokeCheck.Result r = SmokeCheck.check(SERVER_LOG, "1.21.10", SmokeCheck.Side.CLIENT);
        assertFalse(r.ok());
        String client = clientLog();
        assertTrue(SmokeCheck.check(client, "1.21.10", SmokeCheck.Side.CLIENT).ok());
    }

    /** The server log turned into a client log of an integrated server, with the payload round trip and the screenshot. */
    private static String clientLog() {
        return SERVER_LOG.replace("(server)", "(client)")
                .replace("Entrypoint server active (fabric)", "Entrypoint client active (fabric)")
                .replace("resolved server", "resolved client")
                .replace("Server ticked 40 times, stopping\n", "")
                + "[x] (modularity-smoke) Smoke network: server received 42\n"
                + "[x] (modularity-smoke) Smoke network: client received 42\n"
                + "[x] (modularity-smoke) Smoke model: registered for modularity_smoke:feature_block\n"
                + "[x] (modularity-smoke) Smoke client: hud drawn 854x480\n"
                + "[x] (modularity-smoke) Smoke keys: binding=key.modularity_smoke.smoke_key down=false\n"
                + "[x] (modularity-smoke) Smoke obj: parsed modularity_smoke:models/obj/smoke_cube.obj faces=6 groups=1\n"
                + "[x] (modularity-smoke) Smoke obj: drawn modularity_smoke:feature_entity faces=6\n"
                + "[x] (modularity-smoke) Smoke scenario: placed modularity_smoke:feature_block at 3, 64, 6\n"
                + "[x] (modularity-smoke) Smoke model: quads at 3, 64, 6\n"
                + "[x] (modularity-smoke) Smoke particles: hit modularity_smoke:feature_block at 3, 64, 6\n"
                + "[x] (modularity-smoke) Smoke entity: registered for modularity_smoke:feature_entity\n"
                + "[x] (modularity-smoke) Smoke entity: spawned modularity_smoke:feature_entity at 3,64,6\n"
                + "[x] (modularity-smoke) Smoke entity: rendered modularity_smoke:feature_entity\n"
                + "[x] (modularity-smoke) Smoke storage: right-clicked modularity_smoke:feature_entity\n"
                + "[x] (modularity-smoke) Smoke storage: held item moved 1000 mB\n"
                + "[x] (modularity-smoke) Smoke storage: entity tank holds 1000 mB\n"
                + "[x] (modularity-smoke) Smoke storage: bucket took 1000 mB\n"
                + "[x] (modularity-smoke) Smoke storage: bucket took 0 mB\n"
                + "[x] (modularity-smoke) Smoke storage: exposed 500 mB\n"
                + "[x] Client joined world\n[x] Screenshot: Saved screenshot as smoke.png\n[x] Client done\n";
    }

    @Test
    void clientNeedsTheClientSeam() {
        // A run that never draws the HUD, never accepts the model or never registers the key does not prove the seam.
        String noHud = clientLog().replace("Smoke client: hud drawn 854x480\n", "");
        assertTrue(SmokeCheck.check(noHud, "1.21.10", SmokeCheck.Side.CLIENT).problems().stream()
                .anyMatch(p -> p.contains("hud drawn")), "the HUD line must be required");
        String noModel = clientLog().replace("Smoke model: registered for modularity_smoke:feature_block\n", "");
        assertTrue(SmokeCheck.check(noModel, "1.21.10", SmokeCheck.Side.CLIENT).problems().stream()
                .anyMatch(p -> p.contains("Smoke model: registered")), "the model line must be required");
        String noKey = clientLog().replace("Smoke keys: binding=key.modularity_smoke.smoke_key down=false\n", "");
        assertTrue(SmokeCheck.check(noKey, "1.21.10", SmokeCheck.Side.CLIENT).problems().stream()
                .anyMatch(p -> p.contains("Smoke keys: binding")), "the key line must be required");
        // An OBJ model that parsed nothing proves nothing, so an empty model fails the check as well as a missing line.
        String noObj = clientLog().lines().filter(l -> !l.contains("Smoke obj: parsed"))
                .collect(java.util.stream.Collectors.joining("\n", "", "\n"));
        assertTrue(SmokeCheck.check(noObj, "1.21.10", SmokeCheck.Side.CLIENT).problems().stream()
                .anyMatch(p -> p.contains("Smoke obj: parsed")), "the OBJ line must be required");
        String notDrawn = clientLog().replace("Smoke obj: drawn modularity_smoke:feature_entity faces=6\n", "");
        assertTrue(SmokeCheck.check(notDrawn, "1.21.10", SmokeCheck.Side.CLIENT).problems().stream()
                .anyMatch(p -> p.contains("Smoke obj: drawn")), "the OBJ draw line must be required");
        // The two storage paths that need a world: a fill from the held item, and a read through the loader lookup.
        for (String line : new String[]{"Smoke storage: held item moved", "Smoke storage: exposed"}) {
            String without = clientLog().lines().filter(l -> !l.contains(line))
                    .collect(java.util.stream.Collectors.joining("\n", "", "\n"));
            assertTrue(SmokeCheck.check(without, "1.21.10", SmokeCheck.Side.CLIENT).problems().stream()
                    .anyMatch(p -> p.contains(line)), line + " must be required");
        }
        // A lookup that moves nothing proves nothing, so a zero read fails as well as a missing line.
        String emptyLookup = clientLog().replace("Smoke storage: exposed 500 mB", "Smoke storage: exposed 0 mB");
        assertTrue(SmokeCheck.check(emptyLookup, "1.21.10", SmokeCheck.Side.CLIENT).problems().stream()
                .anyMatch(p -> p.contains("Smoke storage: exposed")), "an empty lookup read must fail the check");
        String emptyObj = clientLog().replace("faces=6 groups=1", "faces=0 groups=0");
        assertTrue(SmokeCheck.check(emptyObj, "1.21.10", SmokeCheck.Side.CLIENT).problems().stream()
                .anyMatch(p -> p.contains("Smoke obj: parsed")), "an empty OBJ model must fail the check");
        // The placed block, its rendered quads and the hit particles are three separate proofs of the world paths.
        for (String line : new String[]{"Smoke scenario: placed", "Smoke model: quads", "Smoke particles: hit"}) {
            String without = clientLog().lines().filter(l -> !l.contains(line))
                    .collect(java.util.stream.Collectors.joining("\n", "", "\n"));
            assertTrue(SmokeCheck.check(without, "1.21.10", SmokeCheck.Side.CLIENT).problems().stream()
                    .anyMatch(p -> p.contains(line)), line + " must be required");
        }
        // A dedicated server has no client seam, so its log never needs those lines.
        assertTrue(SmokeCheck.check(SERVER_LOG, "1.21.10", SmokeCheck.Side.SERVER).ok());
    }

    @Test
    void clientNeedsTheEntitySeam() {
        // The entity proves three things in one chain: the type is registered, the server side spawns it, and the
        // client renderer of the loader draws it. A run without one of the three did not prove the chain.
        for (String line : new String[]{"Smoke entity: registered", "Smoke entity: spawned",
                "Smoke entity: rendered"}) {
            String without = clientLog().lines().filter(l -> !l.contains(line))
                    .collect(java.util.stream.Collectors.joining("\n", "", "\n"));
            assertTrue(SmokeCheck.check(without, "1.21.10", SmokeCheck.Side.CLIENT).problems().stream()
                    .anyMatch(p -> p.contains(line)), line + " must be required");
        }
        // No player breaks a block on a dedicated server, so its log never needs those lines.
        assertTrue(SmokeCheck.check(SERVER_LOG, "1.21.10", SmokeCheck.Side.SERVER).ok());
    }

    @Test
    void clientNeedsBothPayloadDirections() {
        // A run that sends but never receives proves one direction only; the check must reject it.
        String noReply = clientLog().replace("Smoke network: client received 42\n", "");
        SmokeCheck.Result r = SmokeCheck.check(noReply, "1.21.10", SmokeCheck.Side.CLIENT);
        assertFalse(r.ok(), r.problems().toString());
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("Smoke network: client received")),
                r.problems().toString());
        // The same run without the server line fails on that direction.
        String noServer = clientLog().replace("Smoke network: server received 42\n", "");
        assertTrue(SmokeCheck.check(noServer, "1.21.10", SmokeCheck.Side.CLIENT).problems().stream()
                .anyMatch(p -> p.contains("Smoke network: server received")));
    }

    @Test
    void clientFrameMustShowSomething() throws Exception {
        // A flat colour, a black screen or no file at all are three different failures of the client render.
        byte[] flat = png(false);
        SmokeCheck.Result r = SmokeCheck.check(clientLog(), "1.21.10", SmokeCheck.Side.CLIENT, "FABRIC", true, flat);
        assertFalse(r.ok(), r.problems().toString());
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("flat screen")), r.problems().toString());
        SmokeCheck.Result ok = SmokeCheck.check(clientLog(), "1.21.10", SmokeCheck.Side.CLIENT, "FABRIC", true, png(true));
        assertTrue(ok.ok(), ok.problems().toString());
        SmokeCheck.Result empty = SmokeCheck.check(clientLog(), "1.21.10", SmokeCheck.Side.CLIENT, "FABRIC", true,
                new byte[0]);
        assertTrue(empty.problems().stream().anyMatch(p -> p.contains("screenshot is empty")), empty.problems().toString());
    }

    @Test
    void serverIgnoresTheScreenshot() throws Exception {
        // Only the client run draws a frame; a server check must not fail because no image was passed.
        assertTrue(SmokeCheck.check(SERVER_LOG, "1.21.10", SmokeCheck.Side.SERVER, "FABRIC", true, png(false)).ok());
    }

    /** A 854x480 RGB frame: one colour, or a gradient that a real render would produce. */
    private static byte[] png(boolean varied) throws Exception {
        java.awt.image.BufferedImage image =
                new java.awt.image.BufferedImage(854, 480, java.awt.image.BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, varied ? (x * 255 / image.getWidth()) << 16 | (y * 255 / image.getHeight()) << 8 : 0x3366CC);
            }
        }
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    @Test
    void dedicatedServerNeedsNoPayloadRoundTrip() {
        // No client connects to the dedicated server, so no message is sent or received there.
        SmokeCheck.Result r = SmokeCheck.check(SERVER_LOG, "1.21.10", SmokeCheck.Side.SERVER);
        assertTrue(r.problems().stream().noneMatch(p -> p.contains("Smoke network")), r.problems().toString());
    }

    @Test
    @DisplayName("A Quilt server passes without the guest lines")
    void quiltServerPassesWithoutGuests() {
        String quilt = SERVER_LOG.replace("(fabric)", "(quilt)")
                .replace("platform fabric:0.20.1", "platform quilt:0.30.2");
        SmokeCheck.Result r = SmokeCheck.check(quilt, "1.21.10", SmokeCheck.Side.SERVER, "QUILT", false);
        assertTrue(r.ok(), r.problems().toString());
    }

    @Test
    @DisplayName("A crash fails the check")
    void crashFails() {
        assertFalse(SmokeCheck.check(SERVER_LOG + "---- Minecraft Crash Report ----\n", "1.21.10", SmokeCheck.Side.SERVER).ok());
    }
}
