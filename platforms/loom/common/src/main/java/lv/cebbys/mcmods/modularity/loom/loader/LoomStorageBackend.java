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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

/** Fabric Transfer API: droplets, entity storages through Modularity's own entity API lookups. */
public final class LoomStorageBackend implements StorageBackend {
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
            FLUID_STORAGE.registerForTypes((entity, ctx) -> LoomTankStorage.of(((ModularEntity) entity).nativeTank()), type);
        }
        if (definition.inventorySlots() > 0) {
            ITEM_STORAGE.registerForTypes((entity, ctx) -> Storages.of(((ModularEntity) entity).nativeInventory().container()), type);
        }
    }

    @Override
    public long fillFromHeldItem(Player player, McFluidTank tank) {
        var source = ContainerItemContext.forPlayerInteraction(player, InteractionHand.MAIN_HAND).find(FluidStorage.ITEM);
        if (source == null) {
            return 0;
        }
        long droplets = StorageUtil.move(source, LoomTankStorage.of(tank), variant -> true, Long.MAX_VALUE, null);
        return droplets * 1000 / FluidConstants.BUCKET;
    }
}
