package lv.cebbys.mcmods.modularity.smoke;

import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * NeoForge side of the storage proof: the tank of a Modularity entity, read through the entity fluid capability that
 * Modularity registers for other mods.
 */
final class NeoForgeStorageProbe {
    private NeoForgeStorageProbe() {
    }

    static void install() {
        LoaderStorageProbes.install((entity, millibuckets) -> {
            IFluidHandler handler = entity.getCapability(Capabilities.FluidHandler.ENTITY, null);
            if (handler == null) {
                return -1L;
            }
            return handler.fill(new FluidStack(Fluids.WATER, millibuckets), IFluidHandler.FluidAction.EXECUTE);
        });
    }
}
