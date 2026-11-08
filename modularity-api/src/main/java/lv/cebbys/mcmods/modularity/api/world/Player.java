package lv.cebbys.mcmods.modularity.api.world;

import lv.cebbys.mcmods.modularity.api.block.BlockStateRef;
import lv.cebbys.mcmods.modularity.api.core.Handle;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;

public interface Player extends Handle {
    /** Mining speed of the held tool against {@code state}, including enchantments and effects. */
    float destroySpeed(BlockStateRef state);

    /** Whether the held tool is correct for drops of {@code state}. */
    boolean hasCorrectToolForDrops(BlockStateRef state);

    boolean isCreative();

    boolean isSpectator();

    ItemStackRef mainHandItem();
}
