package lv.cebbys.mcmods.modularity.loom.loader;

import lv.cebbys.mcmods.modularity.platform.transfer.McFluidTank;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

/** A tank as one Transfer API slot (droplets); changes roll back with the transaction and notify on commit. */
final class ModularityLoomTankStorage extends SnapshotParticipant<McFluidTank.State> implements SingleSlotStorage<FluidVariant> {
    private final McFluidTank tank;

    private ModularityLoomTankStorage(McFluidTank tank) {
        this.tank = tank;
    }

    static ModularityLoomTankStorage of(McFluidTank tank) {
        return tank.view(ModularityLoomTankStorage::new);
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        if (resource.hasComponents()) {
            return 0;
        }
        long inserted = tank.insertable(resource.getFluid(), maxAmount);
        if (inserted > 0) {
            apply(transaction, () -> tank.set(resource.getFluid(), tank.units() + inserted));
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
            apply(transaction, () -> tank.set(tank.nativeFluid(), tank.units() - extracted));
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

    /**
     * Applies {@code change} under {@code transaction}, or at once when the caller passes no context: the Transfer API
     * documents a null context as "no transaction", and {@code updateSnapshots(null)} throws.
     */
    private void apply(TransactionContext transaction, Runnable change) {
        if (transaction == null) {
            change.run();
            tank.changed();
            return;
        }
        updateSnapshots(transaction);
        change.run();
    }

    @Override
    protected void onFinalCommit() {
        tank.changed();
    }
}
