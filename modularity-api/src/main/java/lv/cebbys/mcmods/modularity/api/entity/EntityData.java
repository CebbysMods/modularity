package lv.cebbys.mcmods.modularity.api.entity;

import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.data.DataCodec;

import java.util.Objects;

/**
 * Typed data stored on a Modularity entity. By default it is saved with the world and synced to
 * clients that track the entity; both can be turned off.
 *
 * <pre>{@code
 * static final EntityData<Integer> FUEL = EntityData.of(id("fuel"), DataCodec.INT, 0);
 * static final EntityData<Controls> CONTROLS = EntityData.of(id("controls"), Controls.CODEC, Controls.NONE).notSaved().notSynced();
 * }</pre>
 */
public final class EntityData<T> {
    private final Id id;
    private final DataCodec<T> codec;
    private final T defaultValue;
    private final boolean synced;
    private final boolean saved;

    private EntityData(Id id, DataCodec<T> codec, T defaultValue, boolean synced, boolean saved) {
        this.id = Objects.requireNonNull(id, "id");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        this.synced = synced;
        this.saved = saved;
    }

    public static <T> EntityData<T> of(Id id, DataCodec<T> codec, T defaultValue) {
        return new EntityData<>(id, codec, defaultValue, true, true);
    }

    /** Only the server knows the value (e.g. latest control input). */
    public EntityData<T> notSynced() {
        return new EntityData<>(id, codec, defaultValue, false, saved);
    }

    /** Not written to the world save (transient state). */
    public EntityData<T> notSaved() {
        return new EntityData<>(id, codec, defaultValue, synced, false);
    }

    public Id id() {
        return id;
    }

    public DataCodec<T> codec() {
        return codec;
    }

    public T defaultValue() {
        return defaultValue;
    }

    public boolean synced() {
        return synced;
    }

    public boolean saved() {
        return saved;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof EntityData<?> other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "EntityData[" + id + "]";
    }
}
