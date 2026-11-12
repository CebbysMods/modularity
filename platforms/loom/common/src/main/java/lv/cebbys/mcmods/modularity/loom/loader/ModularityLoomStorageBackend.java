package lv.cebbys.mcmods.modularity.loom.loader;

import lv.cebbys.mcmods.modularity.platform.loader.StorageBackend;
import lv.cebbys.mcmods.modularity.api.entity.EntityDefinition;
import lv.cebbys.mcmods.modularity.platform.compat.Ids;
import lv.cebbys.mcmods.modularity.loom.compat.Storages;
import lv.cebbys.mcmods.modularity.platform.entity.ModularEntity;
import lv.cebbys.mcmods.modularity.platform.transfer.McFluidTank;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Fabric Transfer API: droplets, entity storages through Modularity's own entity API lookups. */
public final class ModularityLoomStorageBackend implements StorageBackend {
    /**
     * Fabric has no standard entity fluid/item lookup, so Modularity provides one. Other mods query
     * {@code EntityApiLookup.get(id("modularity", "fluid_storage"), Storage.asClass(), Void.class)}.
     */
    public static final EntityApiLookup<Storage<FluidVariant>, Void> FLUID_STORAGE = EntityApiLookup.get(
            Ids.of("modularity", "fluid_storage"), Storage.asClass(), Void.class);
    public static final EntityApiLookup<Storage<ItemVariant>, Void> ITEM_STORAGE = EntityApiLookup.get(
            Ids.of("modularity", "item_storage"), Storage.asClass(), Void.class);

    @Override
    public long unitsPerMb() {
        return FluidConstants.BUCKET / 1000;
    }

    @Override
    public void exposeEntityStorages(EntityType<ModularEntity> type, EntityDefinition definition) {
        if (definition.tankCapacity() > 0) {
            FLUID_STORAGE.registerForTypes((entity, ctx) -> ModularityLoomTankStorage.of(((ModularEntity) entity).nativeTank()), type);
        }
        if (definition.inventorySlots() > 0) {
            ITEM_STORAGE.registerForTypes((entity, ctx) -> Storages.of(((ModularEntity) entity).nativeInventory().container()), type);
        }
    }

    @Override
    public long fillFromHeldItem(Player player, McFluidTank tank) {
        var context = ContainerItemContext.forPlayerInteraction(player, InteractionHand.MAIN_HAND);
        var source = context.find(FluidStorage.ITEM);
        if (source == null) {
            return 0;
        }
        long droplets;
        // The move and the swap of the emptied bucket happen in one transaction, so the hand changes only when the
        // fluid reaches the tank.
        try (var transaction = Transaction.openOuter()) {
            droplets = StorageUtil.move(source, ModularityLoomTankStorage.of(tank), variant -> true, Long.MAX_VALUE,
                    transaction);
            if (droplets > 0 && player.getMainHandItem().getItem() instanceof BucketItem) {
                context.exchange(ItemVariant.of(Items.BUCKET), 1, transaction);
            }
            transaction.commit();
        }
        return droplets * 1000 / FluidConstants.BUCKET;
    }

    /** The item a full bucket of {@code fluid} is, or {@code Items.AIR} when the fluid has none. */
    private static Item bucketOf(Fluid fluid) {
        // Water needs no bucket lookup: Quilt hands out a different fluid instance and its bucket item came back blank.
        return fluid == Fluids.WATER ? Items.WATER_BUCKET : fluid.getBucket();
    }

    @Override
    public long drainToHeldItem(Player player, McFluidTank tank) {
        var context = ContainerItemContext.forPlayerInteraction(player, InteractionHand.MAIN_HAND);
        var target = context.find(FluidStorage.ITEM);
        if (target == null) {
            return 0;
        }
        long droplets;
        try (var transaction = Transaction.openOuter()) {
            droplets = StorageUtil.move(ModularityLoomTankStorage.of(tank), target, variant -> true, Long.MAX_VALUE,
                    transaction);
            transaction.commit();
        }
        // The item swap is a second step, so it can never roll back fluid that already left the tank. It also skips a
        // fluid without a bucket item: Quilt reports such a variant as blank and the exchange throws.
        if (droplets > 0 && player.getMainHandItem().getItem() instanceof BucketItem) {
            Item bucket = bucketOf(tank.nativeFluid());
            if (bucket != Items.AIR) {
                try (var transaction = Transaction.openOuter()) {
                    context.exchange(ItemVariant.of(bucket), 1, transaction);
                    transaction.commit();
                }
            }
        }
        return droplets * 1000 / FluidConstants.BUCKET;
    }
}
