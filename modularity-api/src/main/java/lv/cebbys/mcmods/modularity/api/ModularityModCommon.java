package lv.cebbys.mcmods.modularity.api;

/**
 * The common lifecycle of a Modularity mod: content, data and interceptors. It runs on every supported
 * loader and Minecraft version, always before the client and server stages.
 *
 * <p>A mod implements this interface, and {@link ModularityModClient} or {@link ModularityModServer}
 * when it has work for that stage. Every method is default, so a mod overrides only what it needs, and
 * the same instance receives all of its stages.</p>
 *
 * <ul>
 *     <li>Fabric/Quilt: declare it as a {@code "modularity"} entrypoint in {@code fabric.mod.json}.</li>
 *     <li>NeoForge/Forge: call {@link Modularity#registerMod(String, ModularityModCommon)} from the mod's
 *     {@code @Mod} constructor. A generated adapter will do this once the generator exists; until then a
 *     mod calls it directly. Registration must happen before common initialization starts.</li>
 * </ul>
 */
public interface ModularityModCommon {
    /** Common initialization: register content, data, interceptors. Runs on client and server. */
    default void initialize(CommonContext common) {
    }
}
