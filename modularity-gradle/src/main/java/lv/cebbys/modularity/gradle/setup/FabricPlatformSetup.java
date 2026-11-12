package lv.cebbys.modularity.gradle.setup;

import lv.cebbys.modularity.gradle.Loader;

/**
 * Fabric platform: Loom, Minecraft, Fabric Loader, Fabric API, the nested {@code modularity-api}, the separate mods,
 * Mod Menu in development, the smoke runs. Everything but the loader dependency is the shared Loom setup.
 */
class FabricPlatformSetup extends AbstractLoomPlatformSetup {

    @Override
    protected void loaderDependency(DependenciesContext ctx, String modConfig) {
        assert ctx.platform().loader() == Loader.FABRIC;
        ctx.dependencies().add(modConfig, "net.fabricmc:fabric-loader:" + ctx.platform().loaderVersion());
    }
}
