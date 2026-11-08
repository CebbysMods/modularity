package lv.cebbys.mcmods.modularity.api.entity;

import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.world.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Everything a platform needs to create an entity type.
 *
 * <pre>{@code
 * mod.entity("car", e -> e
 *         .size(2.0F, 1.5F)
 *         .trackingRange(128)
 *         .seat(new Vec3(0.4, 0.3, 0.2))
 *         .data(STATE, CONTROLS)
 *         .fluidTank(8000, Id.minecraft("lava"))
 *         .behaviour(new CarBehaviour()));
 * }</pre>
 */
public final class EntityDefinition {
    private final float width;
    private final float height;
    private final int trackingRange;
    private final int updateInterval;
    private final List<Vec3> seats;
    private final Set<EntityData<?>> data;
    private final long tankCapacity;
    private final Set<Id> tankFluids;
    private final int inventorySlots;
    private final boolean solid;
    private final EntityBehaviour behaviour;

    private EntityDefinition(Builder b) {
        this.width = b.width;
        this.height = b.height;
        this.trackingRange = b.trackingRange;
        this.updateInterval = b.updateInterval;
        this.seats = List.copyOf(b.seats);
        this.data = Collections.unmodifiableSet(new LinkedHashSet<>(b.data));
        this.tankCapacity = b.tankCapacity;
        this.tankFluids = Set.copyOf(b.tankFluids);
        this.inventorySlots = b.inventorySlots;
        this.solid = b.solid;
        this.behaviour = b.behaviour;
    }

    public static Builder builder() {
        return new Builder();
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    /** Blocks within which clients receive the entity. */
    public int trackingRange() {
        return trackingRange;
    }

    /** Ticks between position updates sent to clients. */
    public int updateInterval() {
        return updateInterval;
    }

    /** Seat offsets in entity-local space (+z forward, +x left), rotated by the entity yaw. */
    public List<Vec3> seats() {
        return seats;
    }

    public Set<EntityData<?>> data() {
        return data;
    }

    /** 0 when the entity has no tank. */
    public long tankCapacity() {
        return tankCapacity;
    }

    /** Fluids accepted by the tank; empty means any fluid. */
    public Set<Id> tankFluids() {
        return tankFluids;
    }

    /** 0 when the entity has no inventory. */
    public int inventorySlots() {
        return inventorySlots;
    }

    /** Whether other entities collide with it (players can stand on it). */
    public boolean solid() {
        return solid;
    }

    public EntityBehaviour behaviour() {
        return behaviour;
    }

    public static final class Builder {
        private float width = 1.0F;
        private float height = 1.0F;
        private int trackingRange = 80;
        private int updateInterval = 3;
        private final List<Vec3> seats = new ArrayList<>();
        private final List<EntityData<?>> data = new ArrayList<>();
        private long tankCapacity;
        private final Set<Id> tankFluids = new LinkedHashSet<>();
        private int inventorySlots;
        private boolean solid;
        private EntityBehaviour behaviour = EntityBehaviour.NONE;

        private Builder() {
        }

        public Builder size(float width, float height) {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Entity size must be positive");
            }
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder trackingRange(int blocks) {
            this.trackingRange = blocks;
            return this;
        }

        public Builder updateInterval(int ticks) {
            this.updateInterval = Math.max(1, ticks);
            return this;
        }

        /** Adds a seat (first seat = driver/controller). */
        public Builder seat(Vec3 offset) {
            seats.add(Objects.requireNonNull(offset, "offset"));
            return this;
        }

        public Builder data(EntityData<?>... data) {
            this.data.addAll(List.of(data));
            return this;
        }

        public Builder fluidTank(long capacityMillibuckets, Id... acceptedFluids) {
            if (capacityMillibuckets <= 0) {
                throw new IllegalArgumentException("Tank capacity must be positive");
            }
            this.tankCapacity = capacityMillibuckets;
            this.tankFluids.addAll(List.of(acceptedFluids));
            return this;
        }

        public Builder inventory(int slots) {
            if (slots <= 0) {
                throw new IllegalArgumentException("Inventory needs at least one slot");
            }
            this.inventorySlots = slots;
            return this;
        }

        public Builder solid() {
            this.solid = true;
            return this;
        }

        public Builder behaviour(EntityBehaviour behaviour) {
            this.behaviour = Objects.requireNonNull(behaviour, "behaviour");
            return this;
        }

        public EntityDefinition build() {
            return new EntityDefinition(this);
        }
    }
}
