package lv.cebbys.modularity.gradle.setup;

import lv.cebbys.modularity.gradle.Loader;

/**
 * Fabric platform: Loom, Minecraft, Fabric Loader, Fabric API, the nested {@code modularity-api}, the separate mods,
 * Mod Menu in development, the smoke runs.
 */
class FabricPlatformSetup extends AbstractLoomPlatformSetup {

    @Override
    protected void loaderPlugins(LoaderPluginsContext ctx) {
    }

    @Override
    protected void loaderDependency(DependenciesContext ctx, String modConfig) {
        // Fabric Loader on the mod configuration (compileOnly on Quilt).
        assert ctx.platform().loader() == Loader.FABRIC;
    }

    @Override
    protected void dependencies(DependenciesContext ctx) {
    }

    @Override
    protected void runs(RunsContext ctx) {
    }
}
