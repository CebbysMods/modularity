package lv.cebbys.mcmods.modularity.api.world;

import lv.cebbys.mcmods.modularity.api.block.BlockStateRef;
import lv.cebbys.mcmods.modularity.api.data.BlockData;
import lv.cebbys.mcmods.modularity.api.entity.EntityHandle;
import lv.cebbys.mcmods.modularity.api.entity.EntityRef;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;

import java.util.List;

/** A live world (server or client). */
public interface Level extends BlockView {
    boolean isClientSide();

    /** Whether {@code state} placed at {@code pos} would not collide with entities. */
    boolean isUnobstructed(BlockStateRef state, Pos pos);

    /** Stores data on the block at {@code pos}. The block must declare {@code data} in its definition. */
    <T> void setData(Pos pos, BlockData<T> data, T value);

    /** Drops an item entity at the center of {@code pos}. No-op on the client. */
    void dropItem(Pos pos, ItemStackRef stack);

    /** Drops an item entity at {@code at}. No-op on the client. */
    void dropItem(Vec3 at, ItemStackRef stack);

    /**
     * Collision boxes of all blocks intersecting {@code area} (stairs and slabs give their real
     * shapes, air and plants give none). This is the single terrain query physics code needs.
     */
    List<Aabb> blockCollisions(Aabb area);

    /** Spawns a Modularity entity. Server side only. */
    EntityRef spawn(EntityHandle type, Vec3 position, float yaw);

    /** Game time in ticks. */
    long gameTime();
}
