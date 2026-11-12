package lv.cebbys.mcmods.modularity.neoforge.loader;

import lv.cebbys.mcmods.modularity.platform.transfer.McFluidTank;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * One Modularity tank as a NeoForge fluid handler. NeoForge counts millibuckets, and so does a NeoForge
 * {@link McFluidTank} on this loader, so the amounts pass through unchanged.
 */
final class NeoForgeTankHandler implements IFluidHandler {
    private final McFluidTank tank;

    NeoForgeTankHandler(McFluidTank tank) {
        this.tank = tank;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int index) {
        if (tank.nativeFluid() == Fluids.EMPTY || tank.units() == 0) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(tank.nativeFluid(), (int) tank.units());
    }

    @Override
    public int getTankCapacity(int index) {
        return (int) tank.capacityUnits();
    }

    @Override
    public boolean isFluidValid(int index, FluidStack stack) {
        return tank.canInsert(stack.getFluid());
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return 0;
        }
        long inserted = tank.insertable(resource.getFluid(), resource.getAmount());
        if (inserted > 0 && action.execute()) {
            tank.set(resource.getFluid(), tank.units() + inserted);
            tank.changed();
        }
        return (int) inserted;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || resource.getFluid() != tank.nativeFluid()) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        // The fluid is read before the tank changes: an empty tank has no fluid, so reading it after the drain would
        // build an empty stack and report 0 mB although the fluid left (run of 2026-11-12, NeoForge).
        Fluid currentFluid = tank.nativeFluid();
        if (tank.units() == 0 || currentFluid == Fluids.EMPTY || maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        int drained = (int) Math.min(maxDrain, tank.units());
        if (action.execute()) {
            tank.set(currentFluid, tank.units() - drained);
            tank.changed();
        }
        return new FluidStack(currentFluid, drained);
    }
}
