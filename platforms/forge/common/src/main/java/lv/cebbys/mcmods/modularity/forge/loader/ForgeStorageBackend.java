package lv.cebbys.mcmods.modularity.forge.loader;

import lv.cebbys.mcmods.modularity.api.entity.EntityDefinition;
import lv.cebbys.mcmods.modularity.platform.entity.ModularEntity;
import lv.cebbys.mcmods.modularity.platform.loader.StorageBackend;
import lv.cebbys.mcmods.modularity.platform.transfer.McFluidTank;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.wrappers.FluidBucketWrapper;

/**
 * The Forge storage seam: fluid units are millibuckets, and Modularity's entity tank and inventory are exposed as
 * Forge capabilities. Forge attaches a capability to an entity instance, so each {@link #exposeEntityStorages} call
 * adds one listener to the game bus, and the listener gives every entity of that type its own provider.
 */
public final class ForgeStorageBackend implements StorageBackend {
    private static final ResourceLocation STORAGES = ResourceLocation.fromNamespaceAndPath("modularity", "storages");

    @Override
    public long unitsPerMb() {
        return 1;
    }

    @Override
    public void exposeEntityStorages(EntityType<ModularEntity> type, EntityDefinition definition) {
        if (definition.tankCapacity() <= 0 && definition.inventorySlots() <= 0) {
            return;
        }
        // AttachCapabilitiesEvent is generic, so Forge needs the typed overload; addListener refuses it (run of
        // 2026-11-12: "Cannot register a generic event listener with addListener, use addGenericListener").
        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, (AttachCapabilitiesEvent<Entity> event) -> {
            Entity entity = event.getObject();
            if (entity.getType() != type || !(entity instanceof ModularEntity modular)) {
                return;
            }
            event.addCapability(STORAGES, new ForgeEntityStorages(modular, definition));
        });
    }

    @Override
    public long fillFromHeldItem(Player player, McFluidTank tank) {
        ItemStack held = player.getMainHandItem();
        IFluidHandlerItem source = FluidUtil.getFluidHandler(held.copy())
                .orElseGet(() -> held.getItem() instanceof BucketItem ? new FluidBucketWrapper(held.copy()) : null);
        if (source == null) {
            return 0;
        }
        ForgeTankHandler handler = tank.view(ForgeTankHandler::new);
        long before = tank.units();
        FluidStack available = source.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (available.isEmpty()) {
            return 0;
        }
        int accepted = handler.fill(available, IFluidHandler.FluidAction.EXECUTE);
        if (accepted <= 0) {
            return 0;
        }
        source.drain(new FluidStack(available.getFluid(), accepted), IFluidHandler.FluidAction.EXECUTE);
        // The emptied container goes back into the hand, so the player sees the change on this loader.
        player.setItemInHand(InteractionHand.MAIN_HAND, source.getContainer());
        return (tank.units() - before) / unitsPerMb();
    }

    @Override
    public long drainToHeldItem(Player player, McFluidTank tank) {
        ItemStack held = player.getMainHandItem();
        IFluidHandlerItem target = FluidUtil.getFluidHandler(held.copy())
                .orElseGet(() -> held.getItem() instanceof BucketItem ? new FluidBucketWrapper(held.copy()) : null);
        if (target == null) {
            return 0;
        }
        ForgeTankHandler source = tank.view(ForgeTankHandler::new);
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
