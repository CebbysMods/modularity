package lv.cebbys.modularity.gradle.setup;

/**
 * Quilt platform: everything Fabric has, plus Quilt Loader with the libraries of its installer, the mirrored launch
 * properties and the Knot main classes, and without Fabric Loader on the runtime class path.
 */
class QuiltPlatformSetup extends AbstractLoomPlatformSetup {

    @Override
    protected void loaderPlugins(LoaderPluginsContext ctx) {
    }

    @Override
    protected void loaderDependency(DependenciesContext ctx, String modConfig) {
        // Quilt Loader from the Quilt maven, its libraries from quilt_installer.json; Fabric Loader compileOnly.
    }

    @Override
    protected void runtimeConfigurations(DependenciesContext ctx) {
        // Exclude net.fabricmc:fabric-loader from every runtime class path: Quilt Loader ships the same classes.
    }

    @Override
    protected void dependencies(DependenciesContext ctx) {
    }

    @Override
    protected void tasks(TasksContext ctx) {
        // mirrorLaunchProperties: fabric.* launch properties become loader.* in launch.cfg.
    }

    @Override
    protected void runs(RunsContext ctx) {
        // Loom runs, then mainClasses: KnotClient / KnotServer instead of the Fabric launcher.
    }
}
