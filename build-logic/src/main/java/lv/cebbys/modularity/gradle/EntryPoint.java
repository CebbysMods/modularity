package lv.cebbys.modularity.gradle;

/**
 * The entrypoint roles of Modularity, in a fixed order. A class takes a role by implementing the Fabric interface
 * the role names; Fabric itself reads its own key, so the role and the key are kept together here.
 */
public enum EntryPoint {
    COMMON("ModInitializer", "main"),
    CLIENT("ClientModInitializer", "client"),
    SERVER("DedicatedServerModInitializer", "server"),
    LAUNCH("PreLaunchEntrypoint", "preLaunch");

    private final String interfaceName;
    private final String fabricKey;

    EntryPoint(String interfaceName, String fabricKey) {
        this.interfaceName = interfaceName;
        this.fabricKey = fabricKey;
    }

    /** Simple name of the Fabric interface that marks a class as this entrypoint. */
    public String interfaceName() {
        return interfaceName;
    }

    /** Key of the generated {@code fabric.mod.json} that Fabric calls the classes under. */
    public String fabricKey() {
        return fabricKey;
    }
}
