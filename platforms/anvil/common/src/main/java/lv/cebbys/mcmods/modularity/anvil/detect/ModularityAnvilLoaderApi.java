package lv.cebbys.mcmods.modularity.anvil.detect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * The Forge-family mod loader, reached without naming its types. NeoForge and Forge publish the same classes under
 * different packages, so this layer reflects on whichever one is present. NeoForge wins when both are present.
 */
final class ModularityAnvilLoaderApi {
    static final String NEOFORGE_LOADER = "net.neoforged.fml.loading.FMLLoader";
    static final String FORGE_LOADER = "net.minecraftforge.fml.loading.FMLLoader";
    private static final String NEOFORGE_ENVIRONMENT = "net.neoforged.fml.loading.FMLEnvironment";
    private static final String FORGE_ENVIRONMENT = "net.minecraftforge.fml.loading.FMLEnvironment";

    /** Which loader is present: {@code neoforge}, {@code forge}, or empty when none is. */
    enum Family {
        NEOFORGE(NEOFORGE_LOADER, NEOFORGE_ENVIRONMENT, "neoForgeVersion", "neoforge-fml"),
        FORGE(FORGE_LOADER, FORGE_ENVIRONMENT, "forgeVersion", "forge-fml");

        private final String loaderClass;
        private final String environmentClass;
        private final String loaderVersionMethod;
        private final String sourceName;

        Family(String loaderClass, String environmentClass, String loaderVersionMethod, String sourceName) {
            this.loaderClass = loaderClass;
            this.environmentClass = environmentClass;
            this.loaderVersionMethod = loaderVersionMethod;
            this.sourceName = sourceName;
        }

        String sourceName() {
            return sourceName;
        }
    }

    private ModularityAnvilLoaderApi() {
    }

    /** The first family whose loader class loads, or empty. Never initializes the class. */
    static Optional<Family> present() {
        for (Family family : Family.values()) {
            if (loads(family.loaderClass)) {
                return Optional.of(family);
            }
        }
        return Optional.empty();
    }

    static boolean loads(String className) {
        try {
            Class.forName(className, false, ModularityAnvilLoaderApi.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    /** The loader version, the Minecraft version and the development flag, read from the loader's own report. */
    static Optional<String> loaderVersion(Family family) {
        return readString(versionInfo(family), family.loaderVersionMethod);
    }

    static Optional<String> minecraftVersion(Family family) {
        return readString(versionInfo(family), "mcVersion");
    }

    /** True on a dedicated server, false on a client; empty when the environment cannot be read. */
    static Optional<Boolean> client(Family family) {
        try {
            Class<?> type = Class.forName(family.environmentClass, false, ModularityAnvilLoaderApi.class.getClassLoader());
            Field dist = type.getField("dist");
            Object value = dist.get(null);
            return Optional.of("CLIENT".equals(String.valueOf(value)));
        } catch (ReflectiveOperationException | LinkageError e) {
            return Optional.empty();
        }
    }

    /** The loader's own development flag; false when it cannot be read. */
    static boolean development(Family family) {
        try {
            Class<?> type = Class.forName(family.loaderClass, false, ModularityAnvilLoaderApi.class.getClassLoader());
            Method production = type.getMethod("isProduction");
            return !(Boolean) production.invoke(null);
        } catch (ReflectiveOperationException | LinkageError e) {
            return false;
        }
    }

    private static Object versionInfo(Family family) {
        try {
            Class<?> type = Class.forName(family.loaderClass, false, ModularityAnvilLoaderApi.class.getClassLoader());
            return type.getMethod("versionInfo").invoke(null);
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    private static Optional<String> readString(Object target, String accessor) {
        if (target == null) {
            return Optional.empty();
        }
        try {
            Object value = target.getClass().getMethod(accessor).invoke(target);
            return value == null ? Optional.empty() : Optional.of(value.toString());
        } catch (ReflectiveOperationException e) {
            return Optional.empty();
        }
    }
}
