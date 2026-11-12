package lv.cebbys.mcmods.modularity.api.detect;

import lv.cebbys.mcmods.modularity.api.core.MinecraftVersion;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads {@code version.json} from the game jar (present since Minecraft 1.14). Works on every loader
 * and on obfuscated releases, because it needs no game classes.
 */
public final class VersionJsonSource implements VersionSource {
    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
    /** Keys only Minecraft's own version.json has, to skip same-named files of other jars. */
    private static final Pattern MINECRAFT_KEYS = Pattern.compile("\"(protocol_version|world_version)\"");

    private final ClassLoader loader;

    public VersionJsonSource() {
        this(VersionJsonSource.class.getClassLoader());
    }

    VersionJsonSource(ClassLoader loader) {
        this.loader = loader;
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public String name() {
        return "version-json";
    }

    @Override
    public Optional<MinecraftVersion> detect() throws IOException {
        Enumeration<URL> files = loader.getResources("version.json");
        while (files.hasMoreElements()) {
            try (InputStream in = files.nextElement().openStream()) {
                Optional<String> id = parseId(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                if (id.isPresent()) {
                    return Optional.of(MinecraftVersion.parse(id.get()));
                }
            }
        }
        return Optional.empty();
    }

    /** The {@code id} of a Minecraft {@code version.json}, empty for any other JSON. */
    static Optional<String> parseId(String json) {
        if (!MINECRAFT_KEYS.matcher(json).find()) {
            return Optional.empty();
        }
        Matcher matcher = ID.matcher(json);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}
