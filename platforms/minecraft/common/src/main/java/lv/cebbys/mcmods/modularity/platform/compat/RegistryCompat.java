package lv.cebbys.mcmods.modularity.platform.compat;

import lv.cebbys.mcmods.modularity.api.core.Id;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Creating and registering blocks, items and entity types. */
public final class RegistryCompat {
    private RegistryCompat() {
    }

    /** Block properties of the block that will be registered as {@code id}. */
    public static BlockBehaviour.Properties blockProperties(Id id) {
        return BlockBehaviour.Properties.of();
    }

    /** Item properties of the item that will be registered as {@code id}. */
    public static Item.Properties itemProperties(Id id) {
        return new Item.Properties();
    }

    public static <T extends Entity> EntityType<T> build(EntityType.Builder<T> builder, Id id) {
        return builder.build(Ids.rl(id).toString());
    }

    /** A new entity of {@code type}, as spawned by a mod (not a spawner, egg or command). */
    public static <T extends Entity> T create(EntityType<T> type, Level level) {
        return type.create(level);
    }
}
