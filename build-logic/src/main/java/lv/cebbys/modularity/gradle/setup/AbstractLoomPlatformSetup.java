package lv.cebbys.modularity.gradle.setup;

import lv.cebbys.modularity.gradle.PlatformTable;
import lv.cebbys.modularity.gradle.RemapService;
import lv.cebbys.modularity.gradle.smoke.SmokeSetup;
import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;

import java.util.Map;

/**
 * Fabric and Quilt: Loom, Minecraft with mappings, the loader, Fabric API, the mods, the smoke runs. Only the loader
 * dependency and the runtime class path differ between the two.
 */
abstract class AbstractLoomPlatformSetup extends AbstractPlatformSetup {

    /** S1a: Loom, remapping where the Minecraft version is obfuscated. */
    @Override
    protected void loaderPlugins(LoaderPluginsContext ctx) {
        ctx.project().getPluginManager()
                .apply(ctx.platform().remap() ? "net.fabricmc.fabric-loom-remap" : "net.fabricmc.fabric-loom");
    }

    /** S3: Minecraft with its mappings, the loader, Fabric API, the nested modules, the separate mods, Mod Menu. */
    @Override
    protected void dependencies(DependenciesContext ctx) {
        PlatformTable.Platform platform = ctx.platform();
        Project project = ctx.project();
        DependencyHandler deps = ctx.dependencies();

        deps.add("minecraft", "com.mojang:minecraft:" + platform.minecraft());
        if (platform.remap()) {
            deps.add("mappings", mappings(project, platform));
        }
        String modConfig = platform.remap() ? "modImplementation" : "implementation";
        loaderDependency(ctx, modConfig);
        deps.add(modConfig, "net.fabricmc.fabric-api:fabric-api:" + platform.fabricApi());

        for (String path : ctx.mod().nested()) {
            deps.add("api", deps.project(Map.of("path", path)));
            deps.add("include", deps.project(Map.of("path", path)));
        }
        // Companion mods, not nested: declared in fabric.mod.json "depends". Same build and already named,
        // so Loom must not remap (and cache) them.
        for (String companion : ctx.mod().companionMods()) {
            deps.add("implementation", deps.project(Map.of(
                    "path", companionProject(project, PlatformTable.Mod.companionProject(companion, platform.loader())),
                    "configuration", "namedElements")));
        }

        if (platform.modmenu() != null) {
            project.getRepositories().maven(r -> {
                r.setName("TerraformersMC");
                r.setUrl("https://maven.terraformersmc.com/releases/");
                r.content(c -> c.includeGroup("com.terraformersmc"));
            });
            // Development only (runClient): lists the loaded mods.
            deps.add(platform.remap() ? "modLocalRuntime" : "localRuntime", "com.terraformersmc:modmenu:" + platform.modmenu());
        }
        runtimeConfigurations(ctx);
    }

    /** S4: remapping is memory hungry, so a few run at a time across all platforms. */
    @Override
    protected void tasks(TasksContext ctx) {
        var remaps = ctx.project().getGradle().getSharedServices().registerIfAbsent(RemapService.NAME, RemapService.class,
                spec -> spec.getMaxParallelUsages().set(RemapService.MAX_PARALLEL));
        ctx.project().getTasks().matching(t -> t.getName().startsWith("remap")).configureEach(t -> t.usesService(remaps));
    }

    /** S5: Loom runs; each source set is its own mod in development (Fabric's classpath groups). */
    @Override
    protected void runs(RunsContext ctx) {
        LoomGradleExtensionAPI loom = ctx.project().getExtensions().getByType(LoomGradleExtensionAPI.class);
        SmokeSetup.apply(ctx.project(), ctx.platform(), ctx.main(), SmokeSetup.loom(loom));
    }

    /**
     * S3: the loader dependency on the mod configuration ({@code modImplementation} where Minecraft is remapped, else
     * {@code implementation}): Fabric Loader for Fabric, Quilt Loader and its libraries for Quilt.
     */
    protected abstract void loaderDependency(DependenciesContext ctx, String modConfig);

    /** S3: adjustments to the runtime class paths, e.g. Quilt excluding Fabric Loader. */
    protected void runtimeConfigurations(DependenciesContext ctx) {
    }

    /**
     * The mappings of obfuscated Minecraft: official Mojang names, layered with Parchment (parameter names and
     * javadoc) where the table has a Parchment version for it. Parchment is compile-time metadata only: class and
     * member names, the remapped jar and the runtime are the same either way.
     */
    private static Object mappings(Project project, PlatformTable.Platform platform) {
        LoomGradleExtensionAPI loom = project.getExtensions().getByType(LoomGradleExtensionAPI.class);
        if (platform.parchment() == null) {
            return loom.officialMojangMappings();
        }
        parchmentRepository(project);
        return loom.layered(builder -> {
            builder.officialMojangMappings();
            builder.parchment(platform.parchmentArtifact());
        });
    }
}
