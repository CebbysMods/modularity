package lv.cebbys.modularity.gradle.smoke;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

/** Checks the log of the smoke run that just finished, keeps a copy plus a summary, and writes one Allure result. */
public abstract class SmokeCheckTask extends DefaultTask {

    /** {@code logs/latest.log} of the run dir (overwritten by the next run, hence the copy). */
    @Internal
    public abstract RegularFileProperty getLog();

    @Input
    public abstract Property<String> getMinecraft();

    @Input
    public abstract Property<SmokeCheck.Side> getSide();

    /** The loader the detection must report: {@code FABRIC}, {@code QUILT}, {@code NEOFORGE}, {@code FORGE}. */
    @Input
    public abstract Property<String> getPlatform();

    /** Whether the smoke guest mod must load (platforms whose loader supports guest mods). */
    @Input
    public abstract Property<Boolean> getGuests();

    /** The matched lines, one per expectation. */
    @OutputFile
    public abstract RegularFileProperty getSummary();

    /** The PNG the client run wrote ({@code <run dir>/screenshots/smoke.png}); unset for a server run. */
    @Internal
    public abstract RegularFileProperty getScreenshot();

    /** The Allure result and its attachments of this run (docs/07, 7.11). */
    @OutputDirectory
    public abstract DirectoryProperty getResultsDir();

    @TaskAction
    public void check() {
        Path log = getLog().get().getAsFile().toPath();
        SmokeCheck.Side side = getSide().get();
        String minecraft = getMinecraft().get();
        String platform = getPlatform().get();
        boolean guests = getGuests().get();
        List<String> expectations = SmokeCheck.expectations(minecraft, side, platform, guests);
        Path summary = getSummary().get().getAsFile().toPath();
        Path screenshot = getScreenshot().isPresent() ? getScreenshot().get().getAsFile().toPath() : null;

        if (!Files.isRegularFile(log)) {
            // A missing log is still one failed test, so the report keeps the evidence that the run never started.
            String failure = "No smoke log at " + log;
            write(platform, minecraft, side, expectations, null, null, screenshot,
                    new SmokeCheck.Result(List.of(), List.of(failure)), failure);
            throw new GradleException(failure);
        }
        // A client run that forgets the property would silently skip the frame check, so it is an error instead.
        if (side == SmokeCheck.Side.CLIENT && !getScreenshot().isPresent()) {
            throw new GradleException("The client check needs the screenshot property (SmokeSetup sets "
                    + "<run dir>/screenshots/smoke.png)");
        }
        try {
            String text = Files.readString(log, StandardCharsets.UTF_8);
            Files.createDirectories(summary.getParent());
            Files.copy(log, summary.resolveSibling(side.name().toLowerCase(Locale.ROOT) + ".log"),
                    StandardCopyOption.REPLACE_EXISTING);
            // The client check also reads the frame: a `Screenshot:` line alone does not say anything was drawn.
            byte[] png = null;
            if (getScreenshot().isPresent()) {
                Path file = getScreenshot().get().getAsFile().toPath();
                png = Files.isRegularFile(file) ? Files.readAllBytes(file) : new byte[0];
            }
            SmokeCheck.Result result = SmokeCheck.check(text, minecraft, side, platform, guests, png);
            Files.writeString(summary, String.join("\n", result.lines()) + "\n");
            String failure = result.ok() ? null : "Smoke " + side + " " + minecraft + " failed (" + log + "):\n  "
                    + String.join("\n  ", result.problems());
            write(platform, minecraft, side, expectations, log, summary, screenshot, result, failure);
            if (!result.ok()) {
                throw new GradleException(failure);
            }
            getLogger().lifecycle("smoke {} {}: ok\n  {}", side, minecraft, String.join("\n  ", result.lines()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Writes the Allure result before any failure, so a red run keeps its evidence in the report. */
    private void write(String platform, String minecraft, SmokeCheck.Side side, List<String> expectations, Path log,
                       Path summary, Path screenshot, SmokeCheck.Result result, String failureText) {
        SmokeAllureWriter.Run run = new SmokeAllureWriter.Run(platform, minecraft, side.name(), expectations, log,
                summary, screenshot);
        try {
            SmokeAllureWriter.write(run, result, failureText, getResultsDir().get().getAsFile().toPath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
