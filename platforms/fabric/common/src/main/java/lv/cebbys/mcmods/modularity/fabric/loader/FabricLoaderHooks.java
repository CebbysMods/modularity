package lv.cebbys.mcmods.modularity.fabric.loader;

import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.fabric.block.ModularBlockEntity;
import lv.cebbys.mcmods.modularity.fabric.block.ModularEntityBlock;
import lv.cebbys.mcmods.modularity.fabric.compat.CreativeTabs;
import lv.cebbys.mcmods.modularity.fabric.compat.Ids;
import lv.cebbys.mcmods.modularity.fabric.compat.Payloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
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

/** {@link LoaderHooks} with Fabric API: registries are open during init, events for tabs and networking. */
public final class FabricLoaderHooks implements LoaderHooks {

    @Override
    public <T> T register(Registry<? super T> registry, Id id, T value) {
        return Registry.register(registry, Ids.rl(id), value);
    }

    @Override
    public BlockEntityType<ModularBlockEntity> blockEntityType(ModularEntityBlock block) {
        return FabricBlockEntityTypeBuilder
                .<ModularBlockEntity>create((pos, state) -> (ModularBlockEntity) block.newBlockEntity(pos, state), block)
                .build();
    }

    @Override
    public void addToCreativeTab(ResourceKey<CreativeModeTab> tab, Item item) {
        CreativeTabs.add(tab, item);
    }

    @Override
    public <P extends CustomPacketPayload> void registerPayload(CustomPacketPayload.Type<P> type,
                                                                StreamCodec<FriendlyByteBuf, P> codec) {
        Payloads.registerPlay(type, codec);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    @Override
    public <P extends CustomPacketPayload> void onServerPayload(CustomPacketPayload.Type<P> type,
                                                                BiConsumer<ServerPlayer, P> handler) {
        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> handler.accept(context.player(), payload));
    }
}
