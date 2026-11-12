package lv.cebbys.modularity.gradle.smoke;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Checks a smoke run log (docs/07-multi-version-build.md, 7.6). Pure logic, unit tested. */
public final class SmokeCheck {

    public enum Side { SERVER, CLIENT }

    /** The log lines a check found, in expectation order, or the reasons it failed. */
    public record Result(List<String> lines, List<String> problems) {
        public boolean ok() {
            return problems.isEmpty();
        }
    }

    private SmokeCheck() {
    }

    /** Id of the smoke guest mod ({@code modularity-smoke/src/main/resources/modularity.mod.json}). */
    public static final String GUEST = "smoke_guest";

    public static Result check(String log, String minecraft, Side side) {
        return check(log, minecraft, side, "FABRIC", true);
    }

    /**
     * @param platform the loader the detection must report ({@code FABRIC}, {@code QUILT}, {@code NEOFORGE}, {@code FORGE})
     * @param guests   whether the platform loads guest mods (the smoke guest must then load and initialize)
     */
    public static Result check(String log, String minecraft, Side side, String platform, boolean guests) {
        return check(log, minecraft, side, platform, guests, null);
    }

    /**
     * @param screenshot the PNG the client smoke wrote, or {@code null} when the run has no screenshot (a dedicated
     *                   server, or a task that does not pass the file)
     */
    public static Result check(String log, String minecraft, Side side, String platform, boolean guests,
                               byte[] screenshot) {
        List<String> expectations = new ArrayList<>();
        String loader = platform.toLowerCase(Locale.ROOT);
        String family = "\\(" + Pattern.quote(loader) + "\\)";
        String sideName = side.name().toLowerCase(Locale.ROOT);
        expectations.add("Mod modularity: loaded=true version=\\S+ nested=no");
        // The entrypoint stages of every loader: docs/07 7.6, stage order in 7.8.
        if ("neoforge".equals(loader) || "forge".equals(loader)) {
            // The Forge family closes its registries before common setup, so the mod constructor runs the first stages.
            expectations.add("Entrypoint mod-constructor active " + family);
        }
        expectations.add("Entrypoint launch active " + family);
        expectations.add("Entrypoint common active " + family);
        expectations.add("\\(" + sideName + "\\) platform " + loader
                + ":\\S+, minecraft:" + Pattern.quote(minecraft) + "\\b");
        expectations.add("Entrypoint " + sideName + " active " + family);
        // The feature smoke: the smoke mod is a Modularity mod on every loader, so one run proves the registration path
        // of the loader and not only its entry points (docs/07 7.6).
        expectations.add("Smoke features: declared block=\\S+ item=\\S+ channel=\\S+");
        expectations.add("Smoke features: resolved " + sideName + " block=\\S+ present=true");
        // The storage smoke: 1000 mB in, 400 mB out, so the portable view reports 600 mB (docs/07 7.6).
        expectations.add("Smoke storage: unitsPerMb=\\d+ inserted=1000 units=\\d+ drained=400 amount=600");
        // The mixins need no line of their own: only ParticleEngine.crack, which ParticleEngineMixin patches, calls the
        // hit-particle handler, so the client expectation below already fails when a loader does not apply its config
        // (docs/07 7.6, 7.8).
        if (guests) {
            // A guest mod (modularity.mod.json only) was turned into a mod of the loader and its entrypoint ran.
            expectations.add("Loaded guest mod " + GUEST + " ");
            expectations.add("Guest mod " + GUEST + ": loaded=true initialized=true");
        }
        if (side == Side.SERVER) {
            expectations.add("Server ticked \\d+ times, stopping");
        } else {
            // One round trip proves both payload directions (docs/07 7.6): the client sends 42, the server logs it and
            // answers with the same value, the client logs it. Only the client smoke has an integrated server, so a
            // dedicated-server run sends and receives nothing. 42 is SmokeFeatures.MESSAGE.
            expectations.add("Smoke network: server received 42");
            expectations.add("Smoke network: client received 42");
            // The client seam of the loader, in the game: a model decided per position was accepted, the HUD layer
            // drew a frame and the key mapping exists. SmokeFeatures logs one line for each.
            expectations.add("Smoke model: registered for \\S+");
            expectations.add("Smoke client: hud drawn \\d+x\\d+");
            expectations.add("Smoke keys: binding=\\S+ down=(true|false)");
            // The OBJ model path: the reference parsed a Wavefront file out of the client resources, and the entity
            // renderer drew its faces into a frame (docs/07 7.6).
            expectations.add("Smoke obj: parsed \\S+ faces=[1-9]\\d* groups=[1-9]\\d*");
            expectations.add("Smoke obj: drawn \\S+ faces=[1-9]\\d*");
            // The scenario in the joined world: the block exists, the dynamic model was asked for its quads, and the
            // hit-particle handler ran while the block was mined. SmokeScenario logs one line for each.
            expectations.add("Smoke scenario: placed \\S+ at \\S+");
            expectations.add("Smoke model: quads at \\S+");
            expectations.add("Smoke particles: hit \\S+ at \\S+");
            // The entity seam: the break spawns the registered entity on the server, and the entity renderer of the
            // loader draws it on the client. SmokeFeatures logs one line for each.
            expectations.add("Smoke entity: registered for \\S+");
            expectations.add("Smoke entity: spawned \\S+ at \\S+");
            expectations.add("Smoke entity: rendered \\S+");
            // The two storage proofs that need a world (docs/07 7.6): a full bucket moves 1000 mB into a tank through
            // the loader's item path, and the loader's entity lookup accepts 500 mB in the spawned entity's tank.
            // A real player action: the client right-clicks the entity with the bucket in hand.
            expectations.add("Smoke storage: right-clicked \\S+");
            expectations.add("Smoke storage: held item moved 1000 mB");
            // The fluid must land in the entity tank and not in a tank that no running game uses.
            expectations.add("Smoke storage: entity tank holds 1000 mB");
            // The cycle: the empty bucket takes the water out, and a second empty bucket must take nothing.
            expectations.add("Smoke storage: bucket took 1000 mB");
            expectations.add("Smoke storage: bucket took 0 mB");
            expectations.add("Smoke storage: exposed 500 mB");
            expectations.add("Client joined world");
            expectations.add("Screenshot: ");
            expectations.add("Client done");
        }

        List<String> logLines = log.lines().toList();
        List<String> found = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        for (String regex : expectations) {
            Pattern p = Pattern.compile(regex);
            logLines.stream().filter(l -> p.matcher(l).find()).findFirst()
                    .ifPresentOrElse(found::add, () -> problems.add("missing log line /" + regex + "/"));
        }
        // A detection line with another version means the detection is wrong, not just missing.
        Pattern detectedVersion = Pattern.compile("minecraft:" + Pattern.quote(minecraft) + "\\b");
        logLines.stream().filter(l -> l.contains(") platform ") && !detectedVersion.matcher(l).find())
                .findFirst().ifPresent(l -> problems.add("detection reports a different version: " + l));
        // The storage line carries numbers, so the check verifies the arithmetic and not only the line (docs/07 7.6).
        Pattern storage = Pattern.compile(
                "Smoke storage: unitsPerMb=(\\d+) inserted=\\d+ units=(\\d+) drained=\\d+ amount=(\\d+)");
        for (String line : logLines) {
            Matcher m = storage.matcher(line);
            if (!m.find()) {
                continue;
            }
            long unitsPerMb = Long.parseLong(m.group(1));
            long units = Long.parseLong(m.group(2));
            long millibuckets = Long.parseLong(m.group(3));
            if (unitsPerMb < 1) {
                problems.add("storage keeps no loader unit: unitsPerMb=0");
            } else if (units != 1000 * unitsPerMb) {
                problems.add("storage filled " + units + " units where 1000 mB needs " + 1000 * unitsPerMb
                        + " at " + unitsPerMb + " units/mB");
            }
            if (millibuckets != 600) {
                problems.add("storage reports " + millibuckets + " mB after 1000 mB in and 400 mB out");
            }
            break;
        }
        logLines.stream().filter(l -> l.contains("---- Minecraft Crash Report ----") || l.contains("Mixin apply failed")
                        || l.contains("Mixin transformation of") && l.contains("failed"))
                .findFirst().ifPresent(l -> problems.add("crash: " + l.strip()));
        // The `Screenshot:` line only says a file was written. The file itself must show a frame, or the client
        // rendered nothing (docs/07 7.6).
        if (side == Side.CLIENT && screenshot != null) {
            String frame = frameProblem(screenshot);
            if (frame != null) {
                problems.add(frame);
            }
        }
        return new Result(found, problems);
    }

    /**
     * Whether the screenshot shows a rendered frame: a PNG of at least 320x240 that is not one flat colour.
     * It does not prove what the frame holds, only that the client drew a world and not a blank or black screen.
     */
    static String frameProblem(byte[] png) {
        if (png == null || png.length == 0) {
            return "the screenshot is empty";
        }
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(png));
        } catch (IOException e) {
            return "the screenshot is unreadable: " + e.getMessage();
        }
        if (image == null) {
            return "the screenshot is not a PNG this JVM can read";
        }
        if (image.getWidth() < 320 || image.getHeight() < 240) {
            return "the frame is " + image.getWidth() + "x" + image.getHeight() + ", smaller than 320x240";
        }
        Set<Integer> colors = new HashSet<>();
        for (int y = 0; y < image.getHeight(); y += 4) {
            for (int x = 0; x < image.getWidth(); x += 4) {
                colors.add(image.getRGB(x, y));
                if (colors.size() > 16) {
                    return null;
                }
            }
        }
        return "the frame holds " + colors.size() + " colors or fewer, so the client rendered a flat screen";
    }
}
