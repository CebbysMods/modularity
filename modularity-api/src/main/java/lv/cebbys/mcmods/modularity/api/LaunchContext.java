package lv.cebbys.mcmods.modularity.api;

import lv.cebbys.mcmods.modularity.api.core.Side;

import java.util.Optional;

/**
 * Everything a mod can use at the launch stage, before the game starts and before any content exists.
 *
 * <p>The launch stage runs on Fabric and Quilt only, after the loader resolved the mods and before
 * Minecraft loads. The context has no registries and no {@link Platform}, so launch code uses
 * {@code java.*} and these calls only. It must not touch a Minecraft class: that class would load
 * too early.</p>
 *
 * <p>A mod that takes part in this stage also implements {@link ModularityModCommon}: the loader
 * entrypoint list is read by that type, and the same instance receives both stages.</p>
 */
public interface LaunchContext {
    /** The mod id of the mod whose stage runs. */
    String modId();

    /** The logger of this mod, named after its mod id. */
    System.Logger logger();

    /**
     * The side the game will run on, when the loader can tell it this early. Empty when it cannot, so
     * launch code must work without it.
     */
    Optional<Side> side();

    /** A short name for the running loader, for log lines. */
    String loader();
}
