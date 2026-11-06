package lv.cebbys.mcmods.modularity.fabric.loader;

import lv.cebbys.mcmods.modularity.fabric.transfer.McFluidTank;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

/** A tank as one Transfer API slot (droplets); changes roll back with the transaction and notify on commit. */
final class FabricTankStorage extends SnapshotParticipant<McFluidTank.State> implements SingleSlotStorage<FluidVariant> {
    private final McFluidTank tank;

    private FabricTankStorage(McFluidTank tank) {
        this.tank = tank;
    }

    static FabricTankStorage of(McFluidTank tank) {
        return tank.view(FabricTankStorage::new);
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        if (resource.hasComponents()) {
            return 0;
        }
        long inserted = tank.insertable(resource.getFluid(), maxAmount);
        if (inserted > 0) {
            updateSnapshots(transaction);
            tank.set(resource.getFluid(), tank.units() + inserted);
        }
        return inserted;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        if (!resource.equals(getResource())) {
            return 0;
        }
        long extracted = Math.min(maxAmount, tank.units());
        if (extracted > 0) {
            updateSnapshots(transaction);
            tank.set(tank.nativeFluid(), tank.units() - extracted);
        }
        return extracted;
    }

    @Override
    public boolean isResourceBlank() {
        return getResource().isBlank();
    }

    @Override
    public FluidVariant getResource() {
        return FluidVariant.of(tank.nativeFluid());
    }

    @Override
    public long getAmount() {
        return tank.units();
    }

    @Override
    public long getCapacity() {
        return tank.capacityUnits();
    }

    @Override
    protected McFluidTank.State createSnapshot() {
        return tank.state();
    }

    @Override
    protected void readSnapshot(McFluidTank.State snapshot) {
        tank.restore(snapshot);
    }

    @Override
    protected void onFinalCommit() {
        tank.changed();
    }
}
