package lv.cebbys.mcmods.modularity.api.transfer;

import lv.cebbys.mcmods.modularity.api.core.Id;

import java.util.Optional;

/**
 * A single-fluid tank, amounts in millibuckets. Platforms expose tanks through the loader's
 * standard fluid transfer API (Fabric Transfer API, NeoForge capabilities), so pipes and tanks of
 * other mods can fill and drain it.
 */
public interface FluidTank {
    Optional<Id> fluid();

    long amount();

    long capacity();

    /** Whether {@code fluid} may enter this tank. */
    boolean accepts(Id fluid);

    /** @return millibuckets inserted */
    long fill(Id fluid, long millibuckets, boolean simulate);

    /** @return millibuckets removed */
    long drain(long millibuckets, boolean simulate);

    default boolean isEmpty() {
        return amount() <= 0;
    }
}
