package lv.cebbys.mcmods.modularity.api.world;

import lv.cebbys.mcmods.modularity.api.block.BlockStateRef;
import lv.cebbys.mcmods.modularity.api.entity.EntityRef;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.api.transfer.FluidTank;

public interface Player extends EntityRef {
    String name();

    /** Mining speed of the held tool against {@code state}, including enchantments and effects. */
    float destroySpeed(BlockStateRef state);

    /** Whether the held tool is correct for drops of {@code state}. */
    boolean hasCorrectToolForDrops(BlockStateRef state);

    boolean isCreative();

    boolean isSpectator();

    boolean isShiftKeyDown();

    ItemStackRef mainHandItem();

    /**
     * Empties the fluid container in the main hand (bucket, modded tanks...) into {@code tank}
     * through the loader's standard fluid transfer API.
     *
     * @return millibuckets moved
     */
    long fillFromHeldItem(FluidTank tank);

    /** Shows a message, above the hotbar when {@code actionBar}. */
    void displayMessage(String message, boolean actionBar);
}
