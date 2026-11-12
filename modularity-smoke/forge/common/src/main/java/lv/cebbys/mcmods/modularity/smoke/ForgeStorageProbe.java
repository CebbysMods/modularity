package lv.cebbys.mcmods.modularity.smoke;

import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * Forge side of the storage proof: the tank of a Modularity entity, read through the entity fluid capability that
 * Modularity attaches for other mods.
 */
final class ForgeStorageProbe {
    private ForgeStorageProbe() {
    }

    static void install() {
        LoaderStorageProbes.install((entity, millibuckets) -> entity
                .getCapability(ForgeCapabilities.FLUID_HANDLER, null)
                .map(handler -> handler.fill(new FluidStack(Fluids.WATER, millibuckets), IFluidHandler.FluidAction.EXECUTE))
                .map(Integer::longValue)
                .orElse(-1L));
    }
}
