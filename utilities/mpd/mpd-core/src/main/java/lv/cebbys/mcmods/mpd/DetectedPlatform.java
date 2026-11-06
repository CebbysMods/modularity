package lv.cebbys.mcmods.mpd;

import java.util.Objects;

/** The detected mod loader, its version, the side and whether this is a development environment. */
public final class DetectedPlatform {
    /** {@link #platformVersion()} when the source cannot tell the loader version. */
    public static final String UNKNOWN_VERSION = "unknown";

    private final Platform platform;
    private final String platformVersion;
    private final Side side;
    private final boolean development;
    private final String source;

    private DetectedPlatform(Platform platform, String platformVersion, Side side, boolean development, String source) {
        this.platform = Objects.requireNonNull(platform, "platform");
        this.platformVersion = Objects.requireNonNull(platformVersion, "platformVersion");
        this.side = Objects.requireNonNull(side, "side");
        this.development = development;
        this.source = Objects.requireNonNull(source, "source");
    }

    public static DetectedPlatform of(Platform platform, String platformVersion, Side side, boolean development,
                                      String source) {
        return new DetectedPlatform(platform, platformVersion, side, development, source);
    }

    public Platform platform() {
        return platform;
    }

    /** Loader version, e.g. {@code 0.16.9}, or {@link #UNKNOWN_VERSION}. */
    public String platformVersion() {
        return platformVersion;
    }

    public Side side() {
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
        return platform + " " + platformVersion + ", " + side + (development ? " (development)" : "");
    }
}
