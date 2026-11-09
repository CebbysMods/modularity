package lv.cebbys.mcmods.modularity.neoforge.loader;

import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.platform.block.ModularBlockEntity;
import lv.cebbys.mcmods.modularity.platform.block.ModularEntityBlock;
import lv.cebbys.mcmods.modularity.platform.loader.LoaderHooks;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.BiConsumer;

/**
 * The NeoForge content seam. NeoForge registers content through {@code DeferredRegister} and {@code RegisterEvent},
 * which fire before common setup; this seam does not replay those registrations yet, so every method fails loudly
 * instead of registering nothing.
 */
public final class NeoForgeLoaderHooks implements LoaderHooks {

    @Override
    public <T> T register(Registry<? super T> registry, Id id, T value) {
        throw notImplemented("register");
    }

    @Override
    public BlockEntityType<ModularBlockEntity> blockEntityType(ModularEntityBlock block) {
        throw notImplemented("blockEntityType");
    }

    @Override
    public void addToCreativeTab(ResourceKey<CreativeModeTab> tab, Item item) {
        throw notImplemented("addToCreativeTab");
    }

    @Override
    public <P extends CustomPacketPayload> void registerPayload(CustomPacketPayload.Type<P> type,
                                                                StreamCodec<FriendlyByteBuf, P> codec) {
        throw notImplemented("registerPayload");
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        throw notImplemented("sendToPlayer");
    }

    @Override
    public <P extends CustomPacketPayload> void onServerPayload(CustomPacketPayload.Type<P> type,
                                                                BiConsumer<ServerPlayer, P> handler) {
        throw notImplemented("onServerPayload");
    }

    private static UnsupportedOperationException notImplemented(String method) {
        return new UnsupportedOperationException(
                "Modularity content registration is not implemented on NeoForge yet (" + method + ")");
    }
}
