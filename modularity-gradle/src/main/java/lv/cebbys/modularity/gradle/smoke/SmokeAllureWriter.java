package lv.cebbys.modularity.gradle.smoke;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Writes one Allure result for a smoke run (docs/07, 7.11). The file uses the JSON shape of the Allure 2 result files,
 * which the Allure 3 report reads. The build has no JSON library, so the writer builds the text itself.
 */
public final class SmokeAllureWriter {

    /** Everything one smoke run wrote, and the expectation list of {@link SmokeCheck#expectations}. */
    public record Run(String platform, String minecraft, String side, List<String> expectations,
                      Path log, Path summary, Path screenshot) {
    }

    private SmokeAllureWriter() {
    }

    /**
     * Writes the result and its attachments into {@code resultsDir}, after clearing that directory. A repeated build
     * therefore never holds two results for the same run.
     */
    public static void write(Run run, SmokeCheck.Result result, String failureText, Path resultsDir) throws IOException {
        clear(resultsDir);
        Files.createDirectories(resultsDir);
        String uuid = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        List<String> attachments = new ArrayList<>();
        if (isFile(run.log())) {
            attachments.add(attachment(uuid, 1, run.log(), "smoke.log", "text/plain", resultsDir));
        }
        if (isFile(run.summary())) {
            attachments.add(attachment(uuid, 2, run.summary(), "smoke.txt", "text/plain", resultsDir));
        }
        if (isFile(run.screenshot())) {
            attachments.add(attachment(uuid, 3, run.screenshot(), "smoke.png", "image/png", resultsDir));
        }

        String history = "smoke." + run.platform() + "." + run.minecraft() + "." + run.side();
        String status = status(result);
        String labels = String.join(",", List.of(
                label("epic", "Modularity"),
                label("feature", "Smoke Tests"),
                label("story", run.platform() + " " + run.minecraft() + " " + run.side()),
                label("platform", run.platform()),
                label("minecraft", run.minecraft()),
                label("side", run.side()),
                label("tag", "PR-11"),
                label("tag", "PR-12")));
        String parameters = String.join(",", List.of(
                parameter("platform", run.platform()),
                parameter("minecraft", run.minecraft()),
                parameter("side", run.side())));
        String json = "{"
                + "\"uuid\":" + quote(uuid)
                + ",\"historyId\":" + quote(history)
                + ",\"fullName\":" + quote(history)
                + ",\"name\":" + quote("Smoke " + run.platform() + " " + run.minecraft() + " " + run.side())
                + ",\"status\":" + quote(status)
                + ",\"statusDetails\":{\"message\":" + quote(failureText == null ? "" : failureText)
                + ",\"known\":false,\"muted\":false,\"flaky\":false}"
                + ",\"stage\":\"finished\""
                + ",\"labels\":[" + labels + "]"
                + ",\"parameters\":[" + parameters + "]"
                + ",\"steps\":[" + String.join(",", steps(run, result, now)) + "]"
                + ",\"attachments\":[" + String.join(",", attachments) + "]"
                + ",\"start\":" + now + ",\"stop\":" + now
                + "}";
        Files.writeString(resultsDir.resolve(uuid + "-result.json"), json, StandardCharsets.UTF_8);
    }

    /** One step per expectation, in order; a missing line fails its step and shows the line that matched. */
    private static List<String> steps(Run run, SmokeCheck.Result result, long now) {
        List<String> steps = new ArrayList<>();
        List<String> matched = result.lines();
        int index = 0;
        for (String expectation : run.expectations()) {
            boolean missing = result.problems().contains("missing log line /" + expectation + "/");
            StringBuilder step = new StringBuilder();
            step.append("{\"name\":").append(quote(expectation));
            step.append(",\"status\":").append(quote(missing ? "failed" : "passed"));
            step.append(",\"stage\":\"finished\",\"steps\":[],\"attachments\":[],\"parameters\":[");
            if (!missing && index < matched.size()) {
                step.append("{\"name\":\"matched\",\"value\":").append(quote(matched.get(index++))).append('}');
            }
            step.append("],\"start\":").append(now).append(",\"stop\":").append(now).append('}');
            steps.add(step.toString());
        }
        return steps;
    }

    /** {@code passed} when the run is green; {@code broken} for a crash or a missing log; {@code failed} otherwise. */
    private static String status(SmokeCheck.Result result) {
        if (result.ok()) {
            return "passed";
        }
        for (String problem : result.problems()) {
            if (problem.startsWith("crash:") || problem.startsWith("No smoke log")) {
                return "broken";
            }
        }
        return "failed";
    }

    private static String attachment(String uuid, int index, Path source, String name, String type, Path resultsDir)
            throws IOException {
        String fileName = uuid + "-attachment-" + index + suffix(name);
        Files.copy(source, resultsDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        return "{\"name\":" + quote(name) + ",\"source\":" + quote(fileName) + ",\"type\":" + quote(type) + "}";
    }

    private static boolean isFile(Path path) {
        return path != null && Files.isRegularFile(path);
    }

    private static String suffix(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot);
    }

    private static String label(String name, String value) {
        return "{\"name\":" + quote(name) + ",\"value\":" + quote(value) + "}";
    }

    private static String parameter(String name, String value) {
        return "{\"name\":" + quote(name) + ",\"value\":" + quote(value) + "}";
    }

    private static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    private static void clear(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                if (!path.equals(dir)) {
                    Files.delete(path);
                }
            }
        }
    }
}
