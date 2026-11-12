package lv.cebbys.modularity.gradle;

/**
 * The entrypoint roles of Modularity, in a fixed order. A class takes a role by implementing the Fabric interface
 * the role names; each loader reads its own key, so the role and the keys are kept together here.
 */
public enum EntryPoint {
    COMMON("ModInitializer", "main", "main"),
    CLIENT("ClientModInitializer", "client", "client"),
    SERVER("DedicatedServerModInitializer", "server", "server"),
    LAUNCH("PreLaunchEntrypoint", "preLaunch", "pre_launch");

    private final String interfaceName;
    private final String fabricKey;
    private final String quiltKey;

    EntryPoint(String interfaceName, String fabricKey, String quiltKey) {
        this.interfaceName = interfaceName;
        this.fabricKey = fabricKey;
        this.quiltKey = quiltKey;
    }

    /** Simple name of the Fabric interface that marks a class as this entrypoint on every loader. */
    public String interfaceName() {
        return interfaceName;
    }

    /** Key of the generated metadata that the loader calls the classes under. */
    public String key(Loader loader) {
        return loader == Loader.QUILT ? quiltKey : fabricKey;
    }
}
