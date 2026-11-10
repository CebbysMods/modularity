package lv.cebbys.mcmods.modularity.api.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Picks one implementation out of several version specific ones.
 *
 * <pre>{@code
 * Renderer renderer = Versioned.<Renderer>builder()
 *         .since("1.21.2", NewRenderer::new)
 *         .since("1.20", OldRenderer::new)
 *         .select(platform.minecraftVersion());
 * }</pre>
 *
 * The candidate with the highest lower bound that contains the version wins, so the newest
 * implementation that is still compatible is used - older Minecraft versions fall back to older
 * implementations automatically.
 */
public final class Versioned<T> {
    private final List<Candidate<T>> candidates;

    private Versioned(List<Candidate<T>> candidates) {
        this.candidates = List.copyOf(candidates);
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public Optional<T> find(MinecraftVersion version) {
        return candidates.stream()
                .filter(candidate -> candidate.range.contains(version))
                .max(Comparator.comparing(
                        (Candidate<T> c) -> c.range.lowerBound(),
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(candidate -> candidate.factory.get());
    }

    public T select(MinecraftVersion version) {
        return find(version).orElseThrow(() -> new IllegalStateException(
                "No implementation registered for Minecraft " + version + " among " + candidates));
    }

    private record Candidate<T>(VersionRange range, Supplier<? extends T> factory) {
        @Override
        public String toString() {
            return range.toString();
        }
    }

    public static final class Builder<T> {
        private final List<Candidate<T>> candidates = new ArrayList<>();

        public Builder<T> since(String version, Supplier<? extends T> factory) {
            return range(VersionRange.since(version), factory);
        }

        public Builder<T> range(VersionRange range, Supplier<? extends T> factory) {
            candidates.add(new Candidate<>(range, factory));
            return this;
        }

        public Builder<T> fallback(Supplier<? extends T> factory) {
            return range(VersionRange.any(), factory);
        }

        public Versioned<T> build() {
            return new Versioned<>(candidates);
        }

        public T select(MinecraftVersion version) {
            return build().select(version);
        }
    }
}
