package lv.cebbys.mcmods.modularity.api.data;

import lv.cebbys.mcmods.modularity.api.core.Id;

import java.util.Objects;

/**
 * A typed piece of data stored per block position, persisted with the world and synced to clients.
 * It is also readable while rendering, see {@link lv.cebbys.mcmods.modularity.api.world.BlockView#data}.
 *
 * <p>Declare it with {@code mod.blockData("name", codec)} and list it in the block definition with
 * {@code .data(MY_DATA)}. How it is stored (block entity, chunk attachment, ...) is a platform detail.
 */
public final class BlockData<T> {
    private final Id id;
    private final DataCodec<T> codec;

    private BlockData(Id id, DataCodec<T> codec) {
        this.id = Objects.requireNonNull(id, "id");
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    public static <T> BlockData<T> of(Id id, DataCodec<T> codec) {
        return new BlockData<>(id, codec);
    }

    public Id id() {
        return id;
    }

    public DataCodec<T> codec() {
        return codec;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof BlockData<?> other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "BlockData[" + id + "]";
    }
}
