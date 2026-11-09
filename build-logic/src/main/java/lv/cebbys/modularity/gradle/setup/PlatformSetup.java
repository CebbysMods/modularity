package lv.cebbys.modularity.gradle.setup;

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

    public void apply(PlatformSetupContext ctx) {
        var platform = ctx.platform();
        var loader = platform.loader();
        switch (loader) {
            case NEOFORGE ->  this.neoforge.apply(ctx);
            case FABRIC -> this.fabric.apply(ctx);
            case FORGE -> this.forge.apply(ctx);
            case QUILT -> this.quilt.apply(ctx);
            default -> throw new IllegalStateException("Unexpected value: " + loader);
        }
    }
}
