package lv.cebbys.mcmods.modularity.fabric.entity;

import lv.cebbys.mcmods.modularity.fabric.compat.RegistryCompat;
import lv.cebbys.mcmods.modularity.api.entity.EntityDefinition;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Registry of Modularity entity types and their transfer API exposure. */
public final class EntityTypes {
    /**
     * Fabric has no standard entity fluid/item lookup, so Modularity provides one. Other mods query
     * {@code EntityApiLookup.get(id("modularity", "fluid_storage"), Storage.asClass(), Void.class)}.
     */
    public static final EntityApiLookup<Storage<FluidVariant>, Void> FLUID_STORAGE = EntityApiLookup.get(
            ResourceLocation.fromNamespaceAndPath("modularity", "fluid_storage"), Storage.asClass(), Void.class);
    public static final EntityApiLookup<Storage<ItemVariant>, Void> ITEM_STORAGE = EntityApiLookup.get(
            ResourceLocation.fromNamespaceAndPath("modularity", "item_storage"), Storage.asClass(), Void.class);

    private static final Map<EntityType<?>, EntityDefinition> DEFINITIONS = new ConcurrentHashMap<>();
    /** Largest part reach of all multipart types, to widen entity queries (0 = no multipart types). */
    private static volatile double maxPartReach;

    private EntityTypes() {
    }

    public static EntityType<ModularEntity> register(ResourceLocation id, EntityDefinition definition) {
        EntityType<ModularEntity> type = RegistryCompat.build(EntityType.Builder.<ModularEntity>of(ModularEntity::new, MobCategory.MISC)
                .sized(definition.width(), definition.height())
                .clientTrackingRange(Math.max(1, definition.trackingRange() / 16))
                .updateInterval(definition.updateInterval()), id);
        DEFINITIONS.put(type, definition);
        maxPartReach = Math.max(maxPartReach, definition.partReach());
        Registry.register(BuiltInRegistries.ENTITY_TYPE, id, type);
        if (definition.tankCapacity() > 0) {
            FLUID_STORAGE.registerForTypes((entity, ctx) -> ((ModularEntity) entity).nativeTank().storage(), type);
        }
        if (definition.inventorySlots() > 0) {
            ITEM_STORAGE.registerForTypes((entity, ctx) -> ((ModularEntity) entity).nativeInventory().storage(), type);
        }
        return type;
    }

    public static double maxPartReach() {
        return maxPartReach;
    }

    static EntityDefinition definition(EntityType<?> type) {
        EntityDefinition definition = DEFINITIONS.get(type);
        if (definition == null) {
            throw new IllegalStateException("Not a Modularity entity type: " + type);
        }
        return definition;
    }
}
