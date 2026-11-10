package lv.cebbys.mcmods.modularity.api.detect;

import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.api.core.PlatformVersion;
import lv.cebbys.mcmods.modularity.api.core.Side;

import java.util.Objects;
import java.util.Optional;

/** The detected mod loader, its version, the side and whether this is a development environment. */
public final class DetectedPlatform {
    private final Loader loader;
    private final PlatformVersion loaderVersion;
    private final Optional<Side> side;
    private final boolean development;
    private final String source;

    private DetectedPlatform(Loader loader, PlatformVersion loaderVersion, Optional<Side> side, boolean development,
                             String source) {
        this.loader = Objects.requireNonNull(loader, "loader");
        this.loaderVersion = Objects.requireNonNull(loaderVersion, "loaderVersion");
        this.side = Objects.requireNonNull(side, "side");
        this.development = development;
        this.source = Objects.requireNonNull(source, "source");
    }

    /**
     * @param side the physical side, or empty when the source cannot tell it
     */
    public static DetectedPlatform of(Loader loader, PlatformVersion loaderVersion, Optional<Side> side,
                                      boolean development, String source) {
        return new DetectedPlatform(loader, loaderVersion, side, development, source);
    }

    public static DetectedPlatform of(Loader loader, PlatformVersion loaderVersion, Side side, boolean development,
                                      String source) {
        return of(loader, loaderVersion, Optional.ofNullable(side), development, source);
    }

    public Loader loader() {
        return loader;
    }

    /** The loader version, e.g. {@code 0.20.1}, or {@link PlatformVersion#unknown()}. */
    public PlatformVersion loaderVersion() {
        return loaderVersion;
    }

    /** The physical side; empty when only a marker class resolved the loader. */
    public Optional<Side> side() {
        return side;
    }

    public boolean development() {
        return development;
    }

    /** Name of the {@link PlatformSource} that found this platform. */
    public String source() {
        return source;
    }

    /** E.g. {@code FABRIC 0.16.9, CLIENT}. */
    @Override
    public String toString() {
        return loader + " " + loaderVersion + ", " + side.map(Side::toString).orElse("UNKNOWN")
                + (development ? " (development)" : "");
    }
}
