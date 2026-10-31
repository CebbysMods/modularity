package lv.cebbys.mcmods.modularity.api.data;

/**
 * Portable (de)serializer for data stored in the world. Platforms back {@link DataOutput} and
 * {@link DataInput} with NBT, so the storage format stays the same across loaders.
 *
 * <pre>{@code
 * record Owner(String name, int level) {}
 * DataCodec<Owner> CODEC = DataCodec.of(
 *         (owner, out) -> out.putString("name", owner.name()).putInt("level", owner.level()),
 *         in -> new Owner(in.getString("name"), in.getInt("level")));
 * }</pre>
 */
public interface DataCodec<T> {
    void write(T value, DataOutput out);

    T read(DataInput in);

    static <T> DataCodec<T> of(Writer<T> writer, Reader<T> reader) {
        return new DataCodec<>() {
            @Override
            public void write(T value, DataOutput out) {
                writer.write(value, out);
            }

            @Override
            public T read(DataInput in) {
                return reader.read(in);
            }
        };
    }

    @FunctionalInterface
    interface Writer<T> {
        void write(T value, DataOutput out);
    }

    @FunctionalInterface
    interface Reader<T> {
        T read(DataInput in);
    }
}
