package lv.cebbys.modularity.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.initialization.ProjectDescriptor;
import org.gradle.api.initialization.Settings;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Includes one project per platform of {@code gradle/platforms.toml}: {@code :platforms:fabric-<name>} with project
 * dir {@code platforms/fabric/versions/<name>}.
 *
 * <p>{@code -Pmodularity.platforms=none} includes no platform, {@code =21.1,26.3} only those (also when not
 * enabled in the table), the default {@code all} every enabled one.
 */
public class ModularitySettingsPlugin implements Plugin<Settings> {

    public static final String PROPERTY = "modularity.platforms";

    @Override
    public void apply(Settings settings) {
        File root = settings.getRootDir();
        PlatformTable table = PlatformTable.read(root.toPath().resolve(PlatformTable.FILE));
        String selection = settings.getProviders().gradleProperty(PROPERTY).getOrElse("all").trim();

        List<PlatformTable.Platform> selected;
        if (selection.equals("none")) {
            selected = List.of();
        } else if (selection.equals("all")) {
            selected = table.enabled();
        } else {
            List<String> names = Arrays.stream(selection.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
            for (String n : names) {
                if (table.platform(n).isEmpty()) {
                    throw new IllegalArgumentException("-P" + PROPERTY + ": unknown platform '" + n + "' (see "
                            + PlatformTable.FILE + ")");
                }
            }
            selected = table.platforms().stream().filter(p -> names.contains(p.name())).toList();
        }

        for (PlatformTable.Platform p : selected) {
            File dir = new File(root, "platforms/fabric/versions/" + p.name());
            if (!dir.isDirectory() && !dir.mkdirs()) {
                throw new IllegalStateException("Cannot create " + dir);
            }
            settings.include(p.projectPath());
            ProjectDescriptor descriptor = settings.project(p.projectPath());
            descriptor.setProjectDir(dir);
        }
    }
}
