package lv.cebbys.mcmods.modularity.fabric.compat;

import lv.cebbys.mcmods.modularity.api.core.Id;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Creating and registering blocks, items and entity types. 1.21.2: properties and builders need the registry key. */
public final class RegistryCompat {
    private RegistryCompat() {
    }

    /** Block properties of the block that will be registered as {@code id}. */
    public static BlockBehaviour.Properties blockProperties(Id id) {
        return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, Ids.rl(id)));
    }

    /** Item properties of the item that will be registered as {@code id}. */
    public static Item.Properties itemProperties(Id id) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Ids.rl(id)));
    }

    public static <T extends Entity> EntityType<T> build(EntityType.Builder<T> builder, Id id) {
        return builder.build(ResourceKey.create(Registries.ENTITY_TYPE, Ids.rl(id)));
    }

    /** A new entity of {@code type}, as spawned by a mod (not a spawner, egg or command). */
    public static <T extends Entity> T create(EntityType<T> type, Level level) {
        return type.create(level, EntitySpawnReason.TRIGGERED);
    }
}
