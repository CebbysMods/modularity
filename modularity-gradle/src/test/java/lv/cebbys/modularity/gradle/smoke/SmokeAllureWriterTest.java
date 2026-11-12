package lv.cebbys.modularity.gradle.smoke;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Allure result of one smoke run (docs/07 7.11). Plain Java: no Gradle and no Minecraft type. */
@DisplayName("Smoke Allure results")
class SmokeAllureWriterTest {

    @TempDir
    Path base;

    private Path log;
    private Path summary;
    private Path screenshot;
    private Path results;

    @BeforeEach
    void files() throws IOException {
        log = Files.writeString(base.resolve("latest.log"), "server started\n", StandardCharsets.UTF_8);
        summary = Files.writeString(base.resolve("smoke.txt"), "smoke SERVER 1.21.1: ok\n", StandardCharsets.UTF_8);
        screenshot = Files.write(base.resolve("smoke.png"), new byte[]{1, 2, 3});
        results = base.resolve("results");
    }

    @Test
    @DisplayName("A green run is passed and one step per expectation is passed")
    void greenRun() throws IOException {
        SmokeAllureWriter.write(run(List.of("a", "b"), screenshot),
                new SmokeCheck.Result(List.of("a found", "b found"), List.of()), null, results);
        String json = onlyResult();
        assertTrue(json.contains("\"status\":\"passed\""), json);
        assertTrue(json.contains("\"name\":\"a\""), json);
        assertTrue(json.contains("\"name\":\"b\""), json);
        assertTrue(json.contains("{\"name\":\"matched\",\"value\":\"a found\"}"), json);
        assertEquals(3, count(json, "\"status\":\"passed\""));
    }

    @Test
    @DisplayName("A missing expectation fails its step and the run")
    void missingExpectation() throws IOException {
        SmokeAllureWriter.write(run(List.of("a", "b"), null),
                new SmokeCheck.Result(List.of("a found"), List.of("missing log line /b/")), "b is missing", results);
        String json = onlyResult();
        assertTrue(json.contains("\"status\":\"failed\""), json);
        assertTrue(json.contains("\"message\":\"b is missing\""), json);
        assertTrue(json.contains("\"status\":\"passed\""), json);
        assertEquals(2, count(json, "\"status\":\"failed\""));
    }

    @Test
    @DisplayName("A wrong Minecraft version fails the run")
    void wrongVersion() throws IOException {
        SmokeAllureWriter.write(run(List.of("a"), null),
                new SmokeCheck.Result(List.of("a found"),
                        List.of("detection reports a different version: (server) platform fabric:1.20.1")),
                "wrong version", results);
        assertTrue(onlyResult().contains("\"status\":\"failed\""));
    }

    @Test
    @DisplayName("A crash makes the result broken")
    void crashIsBroken() throws IOException {
        SmokeAllureWriter.write(run(List.of("a"), null),
                new SmokeCheck.Result(List.of(), List.of("crash: boom")), "boom", results);
        assertTrue(onlyResult().contains("\"status\":\"broken\""));
    }

    @Test
    @DisplayName("The log, the summary and the client screenshot become attachments")
    void attachments() throws IOException {
        SmokeAllureWriter.write(run(List.of("a"), screenshot),
                new SmokeCheck.Result(List.of("a found"), List.of()), null, results);
        String json = onlyResult();
        assertTrue(json.contains("\"name\":\"smoke.log\""), json);
        assertTrue(json.contains("\"name\":\"smoke.txt\""), json);
        assertTrue(json.contains("\"name\":\"smoke.png\""), json);
        assertTrue(json.contains("\"type\":\"image/png\""), json);
        try (Stream<Path> files = Files.list(results)) {
            List<String> names = files.map(p -> p.getFileName().toString()).toList();
            assertTrue(names.stream().anyMatch(n -> n.endsWith("-attachment-1.log")), names.toString());
            assertTrue(names.stream().anyMatch(n -> n.endsWith("-attachment-2.txt")), names.toString());
            assertTrue(names.stream().anyMatch(n -> n.endsWith("-attachment-3.png")), names.toString());
        }
    }

    @Test
    @DisplayName("A server run carries no screenshot attachment")
    void noScreenshot() throws IOException {
        SmokeAllureWriter.write(run(List.of("a"), null),
                new SmokeCheck.Result(List.of("a found"), List.of()), null, results);
        String json = onlyResult();
        assertFalse(json.contains("smoke.png"), json);
        try (Stream<Path> files = Files.list(results)) {
            assertTrue(files.noneMatch(p -> p.getFileName().toString().endsWith(".png")));
        }
    }

    @Test
    @DisplayName("The result carries its history id, labels and parameters")
    void identifiers() throws IOException {
        SmokeAllureWriter.write(run(List.of("a"), null),
                new SmokeCheck.Result(List.of("a found"), List.of()), null, results);
        String json = onlyResult();
        assertTrue(json.contains("\"historyId\":\"smoke.FABRIC.1.21.1.CLIENT\""), json);
        assertTrue(json.contains("\"value\":\"Smoke Tests\""), json);
        assertTrue(json.contains("\"name\":\"platform\",\"value\":\"FABRIC\""), json);
        assertTrue(json.contains("\"name\":\"side\",\"value\":\"CLIENT\""), json);
    }

    @Test
    @DisplayName("Quotes, backslashes and newlines are escaped")
    void escaping() throws IOException {
        String expectation = "a \"q\" \\ b\nc";
        SmokeAllureWriter.write(run(List.of(expectation), null),
                new SmokeCheck.Result(List.of(expectation), List.of()), null, results);
        String json = onlyResult();
        assertTrue(json.contains("a \\\"q\\\" \\\\ b\\nc"), json);
    }

    @Test
    @DisplayName("A second write leaves one result")
    void repeatedWrite() throws IOException {
        SmokeAllureWriter.write(run(List.of("a"), null),
                new SmokeCheck.Result(List.of("a found"), List.of()), null, results);
        SmokeAllureWriter.write(run(List.of("a"), null),
                new SmokeCheck.Result(List.of("a found"), List.of()), null, results);
        try (Stream<Path> files = Files.list(results)) {
            assertEquals(1, files.filter(p -> p.getFileName().toString().endsWith("-result.json")).count());
        }
    }

    private SmokeAllureWriter.Run run(List<String> expectations, Path screenshot) {
        return new SmokeAllureWriter.Run("FABRIC", "1.21.1", "CLIENT", expectations, log, summary, screenshot);
    }

    private String onlyResult() throws IOException {
        try (Stream<Path> files = Files.list(results)) {
            Path result = files.filter(p -> p.getFileName().toString().endsWith("-result.json")).findFirst()
                    .orElseThrow(() -> new AssertionError("no result file in " + results));
            return Files.readString(result, StandardCharsets.UTF_8);
        }
    }

    private static int count(String text, String needle) {
        int count = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            count++;
        }
        return count;
    }
}
