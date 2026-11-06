package lv.cebbys.mcmods.modularity.platform.loader;

import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.platform.block.ModularBlockEntity;
import lv.cebbys.mcmods.modularity.platform.block.ModularEntityBlock;
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

/** What every loader does its own way on both sides: registering content, creative tabs and network payloads. */
public interface LoaderHooks {

    /** Registers {@code value} under {@code id} in {@code registry} and returns it. */
    <T> T register(Registry<? super T> registry, Id id, T value);

    /** A block entity type for the blocks of {@code block} (not registered yet). */
    BlockEntityType<ModularBlockEntity> blockEntityType(ModularEntityBlock block);

    /** Lists {@code item} in the vanilla creative tab {@code tab}. */
    void addToCreativeTab(ResourceKey<CreativeModeTab> tab, Item item);

    /** Registers {@code type} for both directions of the play phase. */
    <P extends CustomPacketPayload> void registerPayload(CustomPacketPayload.Type<P> type,
                                                         StreamCodec<FriendlyByteBuf, P> codec);

    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    /** Handles {@code type} arriving on the server, on the server thread. */
    <P extends CustomPacketPayload> void onServerPayload(CustomPacketPayload.Type<P> type, BiConsumer<ServerPlayer, P> handler);
}
