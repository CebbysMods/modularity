package lv.cebbys.mcmods.modularity.fabric.loader;

/** The loader behind the Fabric-compatibility entrypoints: Quilt Loader provides the Fabric Loader API. */
public final class FabricLoaderName {

    private FabricLoaderName() {
    }

    /** {@code "quilt"} on Quilt Loader, {@code "fabric"} otherwise. */
    public static String get() {
        return isQuilt() ? "quilt" : "fabric";
    }

    public static boolean isQuilt() {
        try {
            Class.forName("org.quiltmc.loader.api.QuiltLoader", false, FabricLoaderName.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
