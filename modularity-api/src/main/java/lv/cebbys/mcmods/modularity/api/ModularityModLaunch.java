package lv.cebbys.mcmods.modularity.api;

/**
 * The launch lifecycle of a Modularity mod. It runs before the game starts and before
 * {@link ModularityModCommon#initialize(CommonContext)}, on Fabric and Quilt only: the Forge family has
 * no entry point that early.
 *
 * <p>Use it for work that must happen before Minecraft loads, such as setting a system property or
 * loading a native library. Do not register content here; that belongs to the common stage.</p>
 *
 * <p>The method is default, so the implementation may override nothing. A mod that implements this
 * interface also implements {@link ModularityModCommon}, and the same instance receives both stages.</p>
 */
public interface ModularityModLaunch {
    /** Launch initialization. Runs once, before the common stage. */
    default void initialize(LaunchContext launch) {
    }
}
