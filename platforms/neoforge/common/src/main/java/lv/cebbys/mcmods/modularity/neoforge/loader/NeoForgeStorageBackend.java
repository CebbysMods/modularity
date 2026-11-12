package lv.cebbys.mcmods.modularity.neoforge.loader;

import lv.cebbys.mcmods.modularity.api.entity.EntityDefinition;
import lv.cebbys.mcmods.modularity.platform.entity.ModularEntity;
import lv.cebbys.mcmods.modularity.platform.loader.StorageBackend;
import lv.cebbys.mcmods.modularity.platform.transfer.McFluidTank;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

/**
 * The NeoForge storage seam: fluid units are millibuckets, and Modularity's entity tank and inventory are exposed as
 * NeoForge entity capabilities. The capability registration runs in {@link RegisterCapabilitiesEvent}, so each
 * {@link #exposeEntityStorages} call adds a listener that the event replays later.
 */
public final class NeoForgeStorageBackend implements StorageBackend {
    private final IEventBus modBus;

    public NeoForgeStorageBackend(IEventBus modBus) {
        this.modBus = modBus;
    }

    @Override
    public long unitsPerMb() {
        return 1;
    }

    @Override
    public void exposeEntityStorages(EntityType<ModularEntity> type, EntityDefinition definition) {
        if (definition.tankCapacity() > 0) {
            modBus.addListener((RegisterCapabilitiesEvent event) -> event.registerEntity(
                    Capabilities.FluidHandler.ENTITY, type,
                    (entity, direction) -> entity.nativeTank().view(NeoForgeTankHandler::new)));
        }
        if (definition.inventorySlots() > 0) {
            modBus.addListener((RegisterCapabilitiesEvent event) -> event.registerEntity(
                    Capabilities.ItemHandler.ENTITY, type,
                    (entity, direction) -> new InvWrapper(entity.nativeInventory().container())));
        }
    }

    @Override
    public long fillFromHeldItem(Player player, McFluidTank tank) {
        NeoForgeTankHandler handler = tank.view(NeoForgeTankHandler::new);
        long before = tank.units();
        // The tank is the measure, not the result flag: the fluid can reach the tank while the emptied item cannot be
        // stowed, and the caller asks how much fluid moved (run of 2026-11-12: Forge reported 0 with a full tank).
        FluidUtil.tryEmptyContainerAndStow(player.getMainHandItem(), handler,
                new InvWrapper(player.getInventory()), Integer.MAX_VALUE, player, true);
        return (tank.units() - before) / unitsPerMb();
    }

    @Override
    public long drainToHeldItem(Player player, McFluidTank tank) {
        ItemStack held = player.getMainHandItem();
        IFluidHandlerItem target = FluidUtil.getFluidHandler(held.copy())
                .orElseGet(() -> held.getItem() == Items.BUCKET ? new FluidBucketWrapper(held.copy()) : null);
        if (target == null) {
            return 0;
        }
        NeoForgeTankHandler source = tank.view(NeoForgeTankHandler::new);
        FluidStack available = source.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (available.isEmpty()) {
            return 0;
        }
        int moved = target.fill(available, IFluidHandler.FluidAction.EXECUTE);
        if (moved <= 0) {
            return 0;
        }
        source.drain(new FluidStack(available.getFluid(), moved), IFluidHandler.FluidAction.EXECUTE);
        // The filled container goes back into the hand, so the player sees the bucket fill.
        player.setItemInHand(InteractionHand.MAIN_HAND, target.getContainer());
        return moved / unitsPerMb();
    }
}
