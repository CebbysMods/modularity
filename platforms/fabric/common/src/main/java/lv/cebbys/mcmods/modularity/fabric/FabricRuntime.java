package lv.cebbys.mcmods.modularity.fabric;

import lv.cebbys.mcmods.modularity.api.Modularity;
import lv.cebbys.mcmods.modularity.api.block.BlockRef;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.api.core.McVersion;
import lv.cebbys.mcmods.modularity.api.core.Side;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.fabric.compat.Ids;
import lv.cebbys.mcmods.mpd.Platform;
import lv.cebbys.mcmods.mpd.PlatformDetector;
import lv.cebbys.mcmods.mvd.DetectedVersion;
import lv.cebbys.mcmods.mvd.VersionDetector;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

final class FabricRuntime implements Modularity.Runtime, lv.cebbys.mcmods.modularity.api.Platform {
    static final FabricRuntime INSTANCE = new FabricRuntime();

    // Version and loader come from the mvd and mpd utility mods (docs/06-utility-mods.md).
    private final McVersion version = toMcVersion(VersionDetector.detect());
    private final Loader loader = toLoader(PlatformDetector.detect().platform());

    private FabricRuntime() {
    }

    @Override
    public lv.cebbys.mcmods.modularity.api.Platform platform() {
        return this;
    }

    @Override
    public Optional<BlockRef> block(Id id) {
        return BuiltInRegistries.BLOCK.getOptional(Ids.rl(id)).map(Wrap::block);
    }

    @Override
    public ItemStackRef emptyStack() {
        return Wrap.stack(ItemStack.EMPTY);
    }

    @Override
    public Loader loader() {
        return loader;
    }

    @Override
    public McVersion minecraftVersion() {
        return version;
    }

    @Override
    public Side side() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ? Side.CLIENT : Side.SERVER;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public Optional<String> modVersion(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getVersion().getFriendlyString());
    }

    @Override
    public boolean isDevelopment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    static McVersion toMcVersion(DetectedVersion version) {
        return McVersion.of(version.major(), version.minor(), version.patch());
    }

    static Loader toLoader(Platform platform) {
        return switch (platform) {
            case FABRIC -> Loader.FABRIC;
            case QUILT -> Loader.QUILT;
            case NEOFORGE -> Loader.NEOFORGE;
            case FORGE -> Loader.FORGE;
            case UNKNOWN -> throw new IllegalStateException("Minecraft Platform Detector found no known mod loader");
        };
    }
}
