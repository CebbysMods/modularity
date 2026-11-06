package lv.cebbys.modularity.gradle;

import java.util.ArrayList;
import java.util.List;
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

    /** Id of the smoke guest mod ({@code tools/smoke/guest}). */
    public static final String GUEST = "smoke_guest";

    public static Result check(String log, String minecraft, Side side) {
        return check(log, minecraft, side, "FABRIC", true);
    }

    /**
     * @param platform the platform mpd must report ({@code FABRIC}, {@code QUILT}, {@code NEOFORGE}, {@code FORGE})
     * @param guests   whether the platform loads guest mods (the smoke guest must then load and initialize)
     */
    public static Result check(String log, String minecraft, Side side, String platform, boolean guests) {
        List<String> expectations = new ArrayList<>();
        for (String mod : List.of("modularity", "mvd", "mpd")) {
            expectations.add("\\[smoke\\] mod " + mod + ": loaded=true version=\\S+ nested=no");
        }
        expectations.add("\\[mvd\\] Detected version " + Pattern.quote(minecraft) + " \\(");
        expectations.add("\\[mpd\\] Detected platform " + platform + " \\S+, " + side + "\\b");
        if (guests) {
            // A guest mod (modularity.mod.json only) was turned into a mod of the loader and its entrypoint ran.
            expectations.add("\\[modularity\\] Loaded guest mod " + GUEST + " ");
            expectations.add("\\[smoke\\] guest mod " + GUEST + ": loaded=true initialized=true");
        }
        if (side == Side.SERVER) {
            expectations.add("\\[smoke\\] server ticked \\d+ times, stopping");
        } else {
            expectations.add("\\[smoke\\] client joined world");
            expectations.add("\\[smoke\\] screenshot: ");
            expectations.add("\\[smoke\\] client done");
        }

        List<String> logLines = log.lines().toList();
        List<String> found = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        for (String regex : expectations) {
            Pattern p = Pattern.compile(regex);
            logLines.stream().filter(l -> p.matcher(l).find()).findFirst()
                    .ifPresentOrElse(found::add, () -> problems.add("missing log line /" + regex + "/"));
        }
        // An mvd line with another version means the detection is wrong, not just missing.
        logLines.stream().filter(l -> l.contains("[mvd] Detected version ") && !l.contains("version " + minecraft + " "))
                .findFirst().ifPresent(l -> problems.add("mvd reports a different version: " + l));
        logLines.stream().filter(l -> l.contains("---- Minecraft Crash Report ----") || l.contains("Mixin apply failed")
                        || l.contains("Mixin transformation of") && l.contains("failed"))
                .findFirst().ifPresent(l -> problems.add("crash: " + l.strip()));
        return new Result(found, problems);
    }
}
