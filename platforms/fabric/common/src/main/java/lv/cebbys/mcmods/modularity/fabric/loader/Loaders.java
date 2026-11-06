package lv.cebbys.mcmods.modularity.fabric.loader;

import java.util.Objects;

/** The loader seams, installed once by the loader's entrypoint before any mod initializes. */
public final class Loaders {
    private static volatile LoaderInfo info;
    private static volatile RenderDataAccess renderData;

    private Loaders() {
    }

    public static void install(LoaderInfo loaderInfo, RenderDataAccess renderDataAccess) {
        info = Objects.requireNonNull(loaderInfo, "info");
        renderData = Objects.requireNonNull(renderDataAccess, "renderData");
    }

    public static LoaderInfo info() {
        return require(info);
    }

    public static RenderDataAccess renderData() {
        return require(renderData);
    }

    private static <T> T require(T value) {
        if (value == null) {
            throw new IllegalStateException("Modularity loader seams are not installed");
        }
        return value;
    }
}
