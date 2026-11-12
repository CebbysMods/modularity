package lv.cebbys.mcmods.modularity.smoke;

import lv.cebbys.mcmods.modularity.loom.loader.ModularityLoomStorageBackend;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.level.material.Fluids;

/**
 * Fabric side of the storage proof: the tank of a Modularity entity, read through the entity fluid lookup that
 * Modularity registers for other mods.
 */
final class LoomStorageProbe {
    private LoomStorageProbe() {
    }

    static void install() {
        LoaderStorageProbes.install((entity, millibuckets) -> {
            var storage = ModularityLoomStorageBackend.FLUID_STORAGE.find(entity, null);
            if (storage == null) {
                return -1L;
            }
            // A transaction is what another mod would open for a real transfer; the commit makes the change apply.
            try (var transaction = Transaction.openOuter()) {
                long droplets = storage.insert(FluidVariant.of(Fluids.WATER),
                        (long) millibuckets * FluidConstants.BUCKET / 1000, transaction);
                transaction.commit();
                return droplets * 1000 / FluidConstants.BUCKET;
            }
        });
    }
}
