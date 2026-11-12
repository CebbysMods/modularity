package lv.cebbys.mcmods.modularity.loom.detect;

/** Loader API classes the shared Loom sources look for, without loading them. */
final class ModularityLoomClasses {
    static final String FABRIC_LOADER = "net.fabricmc.loader.api.FabricLoader";
    static final String QUILT_LOADER = "org.quiltmc.loader.api.QuiltLoader";

    private ModularityLoomClasses() {
    }

    static boolean exists(String className) {
        try {
            Class.forName(className, false, ModularityLoomClasses.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}
