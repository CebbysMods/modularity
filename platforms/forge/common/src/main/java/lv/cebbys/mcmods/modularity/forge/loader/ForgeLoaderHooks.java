package lv.cebbys.mcmods.modularity.forge.loader;

import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.forge.network.ForgePayloads;
import lv.cebbys.mcmods.modularity.platform.block.ModularBlockEntity;
import lv.cebbys.mcmods.modularity.platform.block.ModularEntityBlock;
import lv.cebbys.mcmods.modularity.platform.compat.Ids;
import lv.cebbys.mcmods.modularity.platform.loader.LoaderHooks;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The Forge content seam. Forge freezes its registries at the end of mod construction and unfreezes them for the
 * {@link RegisterEvent} pass, so a registration is queued here and replayed inside that event, which fires once per
 * registry. A creative tab entry is queued the same way and added inside {@link BuildCreativeModeTabContentsEvent}.
 * The caller must run the common stage before those events fire; the Forge entrypoint runs it on the first
 * {@code RegisterEvent} and then calls {@link #replay(RegisterEvent)} for the registry that event covers, because the
 * listener of this class has already run for it.
 */
public final class ForgeLoaderHooks implements LoaderHooks {
    private final List<Pending> pending = new ArrayList<>();
    private final List<TabEntry> tabs = new ArrayList<>();

    public ForgeLoaderHooks(IEventBus modBus) {
        ForgePayloads.install(modBus);
        modBus.addListener((RegisterEvent event) -> replay(event));
        modBus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            for (TabEntry entry : tabs) {
                if (entry.tab().equals(event.getTabKey())) {
                    event.accept(new ItemStack(entry.item()));
                }
            }
        });
    }

    @Override
    public <T> T register(Registry<? super T> registry, Id id, T value) {
        pending.add(new Pending(registry.key(), Ids.rl(id), value));
        return value;
    }

    @Override
    public BlockEntityType<ModularBlockEntity> blockEntityType(ModularEntityBlock block) {
        return BlockEntityType.Builder
                .of((pos, state) -> (ModularBlockEntity) block.newBlockEntity(pos, state), block)
                .build(null);
    }

    @Override
    public void addToCreativeTab(ResourceKey<CreativeModeTab> tab, Item item) {
        tabs.add(new TabEntry(tab, item));
    }

    @Override
    public <P extends CustomPacketPayload> void registerPayload(CustomPacketPayload.Type<P> type,
                                                                StreamCodec<FriendlyByteBuf, P> codec) {
        ForgePayloads.register(type, codec);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ForgePayloads.sendTo(player, payload);
    }

    @Override
    public <P extends CustomPacketPayload> void onServerPayload(CustomPacketPayload.Type<P> type,
                                                                BiConsumer<ServerPlayer, P> handler) {
        ForgePayloads.onServer(type, handler);
    }

    /** Registers every queued value whose registry this event belongs to. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void replay(RegisterEvent event) {
        ResourceKey<? extends Registry<?>> key = event.getRegistryKey();
        for (Iterator<Pending> it = pending.iterator(); it.hasNext(); ) {
            Pending entry = it.next();
            if (entry.key().equals(key)) {
                event.register((ResourceKey) key, entry.id(), entry::value);
                it.remove();
            }
        }
    }

    private record Pending(ResourceKey<? extends Registry<?>> key, ResourceLocation id, Object value) {
    }

    private record TabEntry(ResourceKey<CreativeModeTab> tab, Item item) {
    }
}
