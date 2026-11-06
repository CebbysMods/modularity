package lv.cebbys.mcmods.modularity.fabric.entity;

import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.entity.EntityDefinition;
import lv.cebbys.mcmods.modularity.fabric.compat.Ids;
import lv.cebbys.mcmods.modularity.fabric.compat.RegistryCompat;
import lv.cebbys.mcmods.modularity.fabric.loader.Loaders;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Registry of Modularity entity types; the loader exposes their tanks and inventories ({@link Loaders#storage()}). */
public final class EntityTypes {
    private static final Map<EntityType<?>, EntityDefinition> DEFINITIONS = new ConcurrentHashMap<>();
    /** Largest part reach of all multipart types, to widen entity queries (0 = no multipart types). */
    private static volatile double maxPartReach;

    private EntityTypes() {
    }

    public static EntityType<ModularEntity> register(Id id, EntityDefinition definition) {
        EntityType<ModularEntity> type = RegistryCompat.build(EntityType.Builder.<ModularEntity>of(ModularEntity::new, MobCategory.MISC)
                .sized(definition.width(), definition.height())
                .clientTrackingRange(Math.max(1, definition.trackingRange() / 16))
                .updateInterval(definition.updateInterval()), id);
        DEFINITIONS.put(type, definition);
        maxPartReach = Math.max(maxPartReach, definition.partReach());
        Registry.register(BuiltInRegistries.ENTITY_TYPE, Ids.rl(id), type);
        Loaders.storage().exposeEntityStorages(type, definition);
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
