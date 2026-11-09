package lv.cebbys.modularity.gradle.setup;

/**
 * Fabric and Quilt: Loom, Minecraft with mappings, the loader, Fabric API, the mods, the smoke runs. Only the loader
 * dependency and the runtime class path differ between the two.
 */
abstract class AbstractLoomPlatformSetup extends AbstractPlatformSetup {

    /**
     * S3: the loader dependency on the mod configuration ({@code modImplementation} where Minecraft is remapped, else
     * {@code implementation}): Fabric Loader for Fabric, Quilt Loader and its libraries for Quilt.
     */
    protected abstract void loaderDependency(DependenciesContext ctx, String modConfig);

    /** S3: adjustments to the runtime class paths, e.g. Quilt excluding Fabric Loader. */
    protected void runtimeConfigurations(DependenciesContext ctx) {
    }
}
