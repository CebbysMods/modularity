package lv.cebbys.mcmods.modularity.api.detect;

import lv.cebbys.mcmods.modularity.api.core.MinecraftVersion;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Asks the game through {@code SharedConstants.getCurrentVersion()} or
 * {@code DetectedVersion.tryDetectVersion()}. Only works where game classes keep their Mojang names
 * (development environments, Minecraft 26.x and later), so it runs last.
 */
public final class ReflectionSource implements VersionSource {
    private final ClassLoader loader;

    public ReflectionSource() {
        this(ReflectionSource.class.getClassLoader());
    }

    ReflectionSource(ClassLoader loader) {
        this.loader = loader;
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public String name() {
        return "reflection";
    }

    @Override
    public Optional<MinecraftVersion> detect() throws ReflectiveOperationException {
        Object version = currentVersion();
        if (version == null) {
            return Optional.empty();
        }
        // WorldVersion#getName() in older versions, #name() once it became a record.
        for (String getter : new String[]{"getName", "name"}) {
            try {
                Method method = version.getClass().getMethod(getter);
                return Optional.of(MinecraftVersion.parse(String.valueOf(method.invoke(version))));
            } catch (NoSuchMethodException ignored) {
                // try the next name
            }
        }
        throw new NoSuchMethodException(version.getClass().getName() + " has neither getName() nor name()");
    }

    private Object currentVersion() throws ReflectiveOperationException {
        Class<?> constants = find("net.minecraft.SharedConstants");
        if (constants != null) {
            return constants.getMethod("getCurrentVersion").invoke(null);
        }
        Class<?> detected = find("net.minecraft.DetectedVersion");
        if (detected != null) {
            return detected.getMethod("tryDetectVersion").invoke(null);
        }
        return null;
    }

    private Class<?> find(String name) {
        try {
            return Class.forName(name, false, loader);
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }
}
