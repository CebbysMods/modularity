package lv.cebbys.mcmods.modularity.fabric;

import lv.cebbys.mcmods.modularity.api.Modularity;
import lv.cebbys.mcmods.modularity.api.Platform;
import lv.cebbys.mcmods.modularity.api.block.BlockRef;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.api.core.McVersion;
import lv.cebbys.mcmods.modularity.api.core.Side;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

final class FabricRuntime implements Modularity.Runtime, Platform {
    static final FabricRuntime INSTANCE = new FabricRuntime();

    private final McVersion version = McVersion.parse(FabricLoader.getInstance().getModContainer("minecraft")
            .orElseThrow().getMetadata().getVersion().getFriendlyString());

    private FabricRuntime() {
    }

    @Override
    public Platform platform() {
        return this;
    }

    @Override
    public Optional<BlockRef> block(Id id) {
        return BuiltInRegistries.BLOCK.getOptional(Wrap.rl(id)).map(Wrap::block);
    }

    @Override
    public ItemStackRef emptyStack() {
        return Wrap.stack(ItemStack.EMPTY);
    }

    @Override
    public Loader loader() {
        return Loader.FABRIC;
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
}
