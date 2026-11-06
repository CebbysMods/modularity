package lv.cebbys.mcmods.modularity.fabric.transfer;

import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.transfer.FluidTank;
import lv.cebbys.mcmods.modularity.fabric.compat.Ids;
import lv.cebbys.mcmods.modularity.fabric.compat.Nbt;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.Optional;
import java.util.Set;

/**
 * Portable tank backed by a Fabric Transfer API storage. The storage is what pipes and other mods
 * see; the portable view converts droplets to millibuckets (1 bucket = 1000 mB = 81000 droplets).
 */
public final class FabricFluidTank implements FluidTank {
    private static final long DROPLETS_PER_MB = FluidConstants.BUCKET / 1000;

    private final long capacityMb;
    private final Set<Id> accepted;
    private final Runnable onChange;
    private final SingleVariantStorage<FluidVariant> storage = new SingleVariantStorage<>() {
        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }

        @Override
        protected long getCapacity(FluidVariant variant) {
            return capacityMb * DROPLETS_PER_MB;
        }

        @Override
        protected boolean canInsert(FluidVariant variant) {
            return accepts(id(variant.getFluid()));
        }

        @Override
        protected void onFinalCommit() {
            onChange.run();
        }
    };

    public FabricFluidTank(long capacityMb, Set<Id> accepted, Runnable onChange) {
        this.capacityMb = capacityMb;
        this.accepted = Set.copyOf(accepted);
        this.onChange = onChange;
    }

    public SingleVariantStorage<FluidVariant> storage() {
        return storage;
    }

    private static Id id(Fluid fluid) {
        return Ids.id(BuiltInRegistries.FLUID.getKey(fluid));
    }

    @Override
    public Optional<Id> fluid() {
        return storage.isResourceBlank() || storage.amount == 0 ? Optional.empty() : Optional.of(id(storage.variant.getFluid()));
    }

    @Override
    public long amount() {
        return storage.amount / DROPLETS_PER_MB;
    }

    @Override
    public long capacity() {
        return capacityMb;
    }

    @Override
    public boolean accepts(Id fluid) {
        return accepted.isEmpty() || accepted.contains(fluid);
    }

    @Override
    public long fill(Id fluid, long millibuckets, boolean simulate) {
        Fluid nativeFluid = BuiltInRegistries.FLUID.getOptional(Ids.rl(fluid)).orElse(Fluids.EMPTY);
        if (nativeFluid == Fluids.EMPTY || millibuckets <= 0) {
            return 0;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            long inserted = storage.insert(FluidVariant.of(nativeFluid), millibuckets * DROPLETS_PER_MB, transaction);
            if (!simulate) {
                transaction.commit();
            }
            return inserted / DROPLETS_PER_MB;
        }
    }

    @Override
    public long drain(long millibuckets, boolean simulate) {
        if (storage.isResourceBlank() || millibuckets <= 0) {
            return 0;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            long extracted = storage.extract(storage.variant, millibuckets * DROPLETS_PER_MB, transaction);
            if (!simulate) {
                transaction.commit();
            }
            return extracted / DROPLETS_PER_MB;
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (!storage.isResourceBlank()) {
            tag.putString("fluid", BuiltInRegistries.FLUID.getKey(storage.variant.getFluid()).toString());
            tag.putLong("amount", storage.amount);
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.contains("fluid")) {
            Fluid fluid = BuiltInRegistries.FLUID.getOptional(Ids.parse(Nbt.getString(tag, "fluid"))).orElse(Fluids.EMPTY);
            storage.variant = FluidVariant.of(fluid);
            storage.amount = Nbt.getLong(tag, "amount");
        } else {
            storage.variant = FluidVariant.blank();
            storage.amount = 0;
        }
    }
}
