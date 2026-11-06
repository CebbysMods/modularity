package lv.cebbys.mcmods.modularity.fabric.loader;

import lv.cebbys.mcmods.modularity.api.entity.EntityDefinition;
import lv.cebbys.mcmods.modularity.fabric.entity.ModularEntity;
import lv.cebbys.mcmods.modularity.fabric.transfer.McFluidTank;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

/**
 * How tanks and inventories meet the loader's transfer API: the unit of fluid amounts, making an entity type's tank
 * and inventory reachable for other mods, and filling a tank from the item a player holds.
 */
public interface StorageBackend {

    /** Loader fluid units per millibucket (Fabric: 81 droplets; NeoForge and Forge: 1). */
    long unitsPerMb();

    /** Lets other mods find the tank and inventory of entities of {@code type}, if {@code definition} has them. */
    void exposeEntityStorages(EntityType<ModularEntity> type, EntityDefinition definition);

    /** Moves fluid from the item in {@code player}'s main hand into {@code tank}; returns millibuckets moved. */
    long fillFromHeldItem(Player player, McFluidTank tank);
}
