package lv.cebbys.mcmods.modularity.api.block;

import lv.cebbys.mcmods.modularity.api.block.property.Property;
import lv.cebbys.mcmods.modularity.api.core.Handle;
import lv.cebbys.mcmods.modularity.api.world.BlockView;
import lv.cebbys.mcmods.modularity.api.world.Pos;

/** Immutable block state. All {@code with} calls return a new state. */
public interface BlockStateRef extends Handle {
    BlockRef block();

    boolean has(Property<?> property);

    <T> T get(Property<T> property);

    <T> BlockStateRef with(Property<T> property, T value);

    /** Block hardness ({@code -1} for unbreakable blocks). */
    float destroySpeed(BlockView view, Pos pos);

    int lightEmission();

    boolean is(BlockTag tag);

    default boolean is(BlockRef block) {
        return block().id().equals(block.id());
    }

    default boolean isAir() {
        return block().id().equals(lv.cebbys.mcmods.modularity.api.core.Id.minecraft("air"));
    }
}
