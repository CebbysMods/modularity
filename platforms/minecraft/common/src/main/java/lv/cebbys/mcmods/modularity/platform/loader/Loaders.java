package lv.cebbys.mcmods.modularity.platform.loader;

import java.util.Objects;

/** The loader seams, installed once by the loader's entrypoint before any mod initializes. */
public final class Loaders {
    private static volatile LoaderInfo info;
    private static volatile RenderDataAccess renderData;
    private static volatile StorageBackend storage;
    private static volatile LoaderHooks hooks;
    private static volatile ClientHooks client;

    private Loaders() {
    }

    public static void install(LoaderInfo loaderInfo, RenderDataAccess renderDataAccess, StorageBackend storageBackend,
                               LoaderHooks loaderHooks) {
        info = Objects.requireNonNull(loaderInfo, "info");
        renderData = Objects.requireNonNull(renderDataAccess, "renderData");
        storage = Objects.requireNonNull(storageBackend, "storage");
        hooks = Objects.requireNonNull(loaderHooks, "hooks");
    }

    /** Installed by the client entrypoint only. */
    public static void installClient(ClientHooks clientHooks) {
        client = Objects.requireNonNull(clientHooks, "client");
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

    public static LoaderHooks hooks() {
        return require(hooks);
    }

    /** The client hooks; fails on a dedicated server. */
    public static ClientHooks client() {
        return require(client);
    }

    private static <T> T require(T value) {
        if (value == null) {
            throw new IllegalStateException("Modularity loader seams are not installed");
        }
        return value;
    }
}
