package lv.cebbys.mcmods.modularity.platform.transfer;

import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.transfer.FluidTank;
import lv.cebbys.mcmods.modularity.platform.compat.Ids;
import lv.cebbys.mcmods.modularity.platform.compat.Nbt;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * One fluid tank, the same on every loader. Amounts are kept in the loader's unit ({@code unitsPerMb}: Fabric
 * droplets, 81 per mB; 1 elsewhere), so other mods see exact amounts; the portable view is in millibuckets. Other
 * mods reach it through the loader's own storage API ({@link #view}).
 */
public final class McFluidTank implements FluidTank {
    private final long unitsPerMb;
    private final long capacityMb;
    private final Set<Id> accepted;
    private final Runnable onChange;
    private Fluid fluid = Fluids.EMPTY;
    private long amount;
    private Object view;

    /** The tank's content, for loader transactions to roll back. */
    public record State(Fluid fluid, long amount) {
    }

    public McFluidTank(long unitsPerMb, long capacityMb, Set<Id> accepted, Runnable onChange) {
        this.unitsPerMb = unitsPerMb;
        this.capacityMb = capacityMb;
        this.accepted = Set.copyOf(accepted);
        this.onChange = onChange;
    }

    private static Id id(Fluid fluid) {
        return Ids.id(BuiltInRegistries.FLUID.getKey(fluid));
    }

    // ------------------------------------------------------------------ loader-unit view

    public Fluid nativeFluid() {
        return fluid;
    }

    public long units() {
        return amount;
    }

    public long capacityUnits() {
        return capacityMb * unitsPerMb;
    }

    /** Whether {@code candidate} may go in now: accepted, and the tank is empty or holds it already. */
    public boolean canInsert(Fluid candidate) {
        return candidate != Fluids.EMPTY && accepts(id(candidate)) && (fluid == Fluids.EMPTY || fluid == candidate);
    }

    /** Units of {@code candidate} that would fit, at most {@code max}. */
    public long insertable(Fluid candidate, long max) {
        return canInsert(candidate) ? Math.max(0, Math.min(max, capacityUnits() - amount)) : 0;
    }

    /** Sets the content without notifying (loader transactions notify on commit, see {@link #changed()}). */
    public void set(Fluid newFluid, long newAmount) {
        this.amount = newAmount;
        this.fluid = newAmount <= 0 ? Fluids.EMPTY : newFluid;
    }

    public State state() {
        return new State(fluid, amount);
    }

    public void restore(State state) {
        set(state.fluid(), state.amount());
    }

    public void changed() {
        onChange.run();
    }

    /** The loader's storage object over this tank, created once (the same instance across a transaction). */
    @SuppressWarnings("unchecked")
    public synchronized <T> T view(Function<McFluidTank, T> create) {
        if (view == null) {
            view = create.apply(this);
        }
        return (T) view;
    }

    // ------------------------------------------------------------------ portable view

    @Override
    public Optional<Id> fluid() {
        return fluid == Fluids.EMPTY || amount == 0 ? Optional.empty() : Optional.of(id(fluid));
    }

    @Override
    public long amount() {
        return amount / unitsPerMb;
    }

    @Override
    public long capacity() {
        return capacityMb;
    }

    @Override
    public boolean accepts(Id candidate) {
        return accepted.isEmpty() || accepted.contains(candidate);
    }

    @Override
    public long fill(Id fluidId, long millibuckets, boolean simulate) {
        Fluid nativeFluid = BuiltInRegistries.FLUID.getOptional(Ids.rl(fluidId)).orElse(Fluids.EMPTY);
        if (nativeFluid == Fluids.EMPTY || millibuckets <= 0) {
            return 0;
        }
        long inserted = insertable(nativeFluid, millibuckets * unitsPerMb);
        if (!simulate && inserted > 0) {
            set(nativeFluid, amount + inserted);
            changed();
        }
        return inserted / unitsPerMb;
    }

    @Override
    public long drain(long millibuckets, boolean simulate) {
        if (fluid == Fluids.EMPTY || millibuckets <= 0) {
            return 0;
        }
        long extracted = Math.min(millibuckets * unitsPerMb, amount);
        if (!simulate && extracted > 0) {
            set(fluid, amount - extracted);
            changed();
        }
        return extracted / unitsPerMb;
    }

    // ------------------------------------------------------------------ saving

    /** {@code fluid} (id) and {@code amount} (loader units); empty tag when empty. */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (fluid != Fluids.EMPTY) {
            tag.putString("fluid", BuiltInRegistries.FLUID.getKey(fluid).toString());
            tag.putLong("amount", amount);
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.contains("fluid")) {
            set(BuiltInRegistries.FLUID.getOptional(Ids.parse(Nbt.getString(tag, "fluid"))).orElse(Fluids.EMPTY),
                    Nbt.getLong(tag, "amount"));
        } else {
            set(Fluids.EMPTY, 0);
        }
    }
}
