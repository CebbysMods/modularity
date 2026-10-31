package lv.cebbys.mcmods.modularity.api.transfer;

import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.data.DataInput;
import lv.cebbys.mcmods.modularity.api.data.DataOutput;

import java.util.Optional;
import java.util.Set;

/** Plain in-memory {@link FluidTank}. */
public class SimpleFluidTank implements FluidTank {
    private final long capacity;
    private final Set<Id> accepted;
    private Id fluid;
    private long amount;

    public SimpleFluidTank(long capacity, Set<Id> accepted) {
        this.capacity = capacity;
        this.accepted = Set.copyOf(accepted);
    }

    @Override
    public Optional<Id> fluid() {
        return amount > 0 ? Optional.of(fluid) : Optional.empty();
    }

    @Override
    public long amount() {
        return amount;
    }

    @Override
    public long capacity() {
        return capacity;
    }

    @Override
    public boolean accepts(Id candidate) {
        return (accepted.isEmpty() || accepted.contains(candidate)) && (amount == 0 || candidate.equals(fluid));
    }

    @Override
    public long fill(Id candidate, long millibuckets, boolean simulate) {
        if (millibuckets <= 0 || !accepts(candidate)) {
            return 0;
        }
        long moved = Math.min(millibuckets, capacity - amount);
        if (!simulate && moved > 0) {
            fluid = candidate;
            amount += moved;
            onChanged();
        }
        return moved;
    }

    @Override
    public long drain(long millibuckets, boolean simulate) {
        long moved = Math.min(Math.max(0, millibuckets), amount);
        if (!simulate && moved > 0) {
            amount -= moved;
            onChanged();
        }
        return moved;
    }

    protected void onChanged() {
    }

    public void write(DataOutput out) {
        out.putInt("amount", (int) amount);
        if (fluid != null) {
            out.putId("fluid", fluid);
        }
    }

    public void read(DataInput in) {
        amount = in.has("amount") ? in.getInt("amount") : 0;
        fluid = in.has("fluid") ? in.getId("fluid") : null;
    }
}
