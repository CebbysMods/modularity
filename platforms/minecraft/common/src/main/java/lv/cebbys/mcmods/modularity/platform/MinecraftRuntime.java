package lv.cebbys.mcmods.modularity.platform;

import lv.cebbys.mcmods.modularity.api.Modularity;
import lv.cebbys.mcmods.modularity.api.block.BlockRef;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.api.core.MinecraftVersion;
import lv.cebbys.mcmods.modularity.api.core.Side;
import lv.cebbys.mcmods.modularity.api.detect.PlatformDetector;
import lv.cebbys.mcmods.modularity.api.detect.VersionDetector;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.platform.compat.Ids;
import lv.cebbys.mcmods.modularity.platform.loader.Loaders;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** The Modularity runtime and platform info, the same on every loader; loader facts come from {@link Loaders#info()}. */
public final class MinecraftRuntime implements Modularity.Runtime, lv.cebbys.mcmods.modularity.api.Platform {
    public static final MinecraftRuntime INSTANCE = new MinecraftRuntime();

    // Version and loader come from the sources the loader entrypoint installs (api.detect).
    private final MinecraftVersion version = VersionDetector.detect();
    private final Loader loader = PlatformDetector.detect().loader();

    private MinecraftRuntime() {
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
    public MinecraftVersion minecraftVersion() {
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
}
