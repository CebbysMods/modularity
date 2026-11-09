package lv.cebbys.modularity.gradle.smoke;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Checks the log of the smoke run that just finished and keeps a copy plus a summary next to it. */
public abstract class SmokeCheckTask extends DefaultTask {

    /** {@code logs/latest.log} of the run dir (overwritten by the next run, hence the copy). */
    @Internal
    public abstract RegularFileProperty getLog();

    @Input
    public abstract Property<String> getMinecraft();

    @Input
    public abstract Property<SmokeCheck.Side> getSide();

    /** Platform mpd must report: {@code FABRIC}, {@code QUILT}, {@code NEOFORGE}, {@code FORGE}. */
    @Input
    public abstract Property<String> getPlatform();

    /** Whether the smoke guest mod must load (platforms whose loader supports guest mods). */
    @Input
    public abstract Property<Boolean> getGuests();

    /** The matched lines, one per expectation. */
    @OutputFile
    public abstract RegularFileProperty getSummary();


    @TaskAction
    public void check() {
        Path log = getLog().get().getAsFile().toPath();
        if (!Files.isRegularFile(log)) {
            throw new GradleException("No smoke log at " + log);
        }
        try {
            String text = new String(Files.readAllBytes(log), StandardCharsets.UTF_8);
            Path summary = getSummary().get().getAsFile().toPath();
            Files.createDirectories(summary.getParent());
            Files.copy(log, summary.resolveSibling(getSide().get().name().toLowerCase() + ".log"),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            SmokeCheck.Result result = SmokeCheck.check(text, getMinecraft().get(), getSide().get(), getPlatform().get(),
                    getGuests().get());
            Files.writeString(summary, String.join("\n", result.lines()) + "\n");
            if (!result.ok()) {
                throw new GradleException("Smoke " + getSide().get() + " " + getMinecraft().get() + " failed ("
                        + log + "):\n  " + String.join("\n  ", result.problems()));
            }
            getLogger().lifecycle("smoke {} {}: ok\n  {}", getSide().get(), getMinecraft().get(),
                    String.join("\n  ", result.lines()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
