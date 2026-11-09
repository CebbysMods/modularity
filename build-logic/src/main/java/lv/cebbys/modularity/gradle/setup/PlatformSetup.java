package lv.cebbys.modularity.gradle.setup;

import lv.cebbys.modularity.gradle.PlatformTable;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;

/** Dispatches one platform project to the setup of its loader. */
public class PlatformSetup {
    private final NeoForgePlatformSetup neoforge;
    private final FabricPlatformSetup fabric;
    private final ForgePlatformSetup forge;
    private final QuiltPlatformSetup quilt;

    public PlatformSetup() {
        this.neoforge = new NeoForgePlatformSetup();
        this.fabric = new FabricPlatformSetup();
        this.forge = new ForgePlatformSetup();
        this.quilt = new QuiltPlatformSetup();
    }

    public void apply(Project project, PlatformTable table, PlatformTable.Platform platform, SourceSet main) {
        var ctx = new AbstractPlatformSetup.PlatformSetupContext(project, table, platform, main);
        switch (platform.loader()) {
            case NEOFORGE -> this.neoforge.apply(ctx);
            case FABRIC -> this.fabric.apply(ctx);
            case FORGE -> this.forge.apply(ctx);
            case QUILT -> this.quilt.apply(ctx);
        }
    }
}
