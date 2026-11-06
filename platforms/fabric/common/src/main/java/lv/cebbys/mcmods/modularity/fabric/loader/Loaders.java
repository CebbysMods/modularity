package lv.cebbys.mcmods.modularity.fabric.loader;

import java.util.Objects;

/** The loader seams, installed once by the loader's entrypoint before any mod initializes. */
public final class Loaders {
    private static volatile LoaderInfo info;
    private static volatile RenderDataAccess renderData;
    private static volatile StorageBackend storage;

    private Loaders() {
    }

    public static void install(LoaderInfo loaderInfo, RenderDataAccess renderDataAccess, StorageBackend storageBackend) {
        info = Objects.requireNonNull(loaderInfo, "info");
        renderData = Objects.requireNonNull(renderDataAccess, "renderData");
        storage = Objects.requireNonNull(storageBackend, "storage");
    }

    public static LoaderInfo info() {
        return require(info);
    }

    public static RenderDataAccess renderData() {
        return require(renderData);
    }

    public static StorageBackend storage() {
        return require(storage);
    }

    private static <T> T require(T value) {
        if (value == null) {
            throw new IllegalStateException("Modularity loader seams are not installed");
        }
        return value;
    }
}
