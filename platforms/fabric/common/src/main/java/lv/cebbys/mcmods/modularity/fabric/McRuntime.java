package lv.cebbys.mcmods.modularity.fabric;

import lv.cebbys.mcmods.modularity.api.Modularity;
import lv.cebbys.mcmods.modularity.api.block.BlockRef;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.api.core.McVersion;
import lv.cebbys.mcmods.modularity.api.core.Side;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.fabric.compat.Ids;
import lv.cebbys.mcmods.modularity.fabric.loader.Loaders;
import lv.cebbys.mcmods.mpd.Platform;
import lv.cebbys.mcmods.mpd.PlatformDetector;
import lv.cebbys.mcmods.mvd.DetectedVersion;
import lv.cebbys.mcmods.mvd.VersionDetector;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** The Modularity runtime and platform info, the same on every loader; loader facts come from {@link Loaders#info()}. */
public final class McRuntime implements Modularity.Runtime, lv.cebbys.mcmods.modularity.api.Platform {
    public static final McRuntime INSTANCE = new McRuntime();

    // Version and loader come from the mvd and mpd utility mods (docs/06-utility-mods.md).
    private final McVersion version = toMcVersion(VersionDetector.detect());
    private final Loader loader = toLoader(PlatformDetector.detect().platform());

    private McRuntime() {
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
        return Loaders.info().side();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return Loaders.info().isModLoaded(modId);
    }

    @Override
    public Optional<String> modVersion(String modId) {
        return Loaders.info().modVersion(modId);
    }

    @Override
    public boolean isDevelopment() {
        return Loaders.info().isDevelopment();
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
