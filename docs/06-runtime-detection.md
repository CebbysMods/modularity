# 6. Runtime detection

Modularity must know which Minecraft version and which mod loader it runs on. The detection lives in
Modularity itself, in `modularity-api`; no separate detector mod is needed.

## 6.1 Where the code lives

| Layer | Package | Content |
|---|---|---|
| `modularity-api` | `api.core` | `Version`, `MinecraftVersion`, `PlatformVersion`, `Loader`, `Side` |
| `modularity-api` | `api.detect` | `VersionDetector`, `PlatformDetector`, the source interfaces, `DetectedPlatform`, the built-in sources and the exceptions |
| `platforms/loom` | `loom.detect` | `LoomVersionSource`, `LoomPlatformSource` (Fabric Loader API; Quilt Loader provides it) |
| `platforms/neoforge` | `neoforge.detect` | `NeoForgeVersionSource`, `NeoForgePlatformSource` |
| `platforms/forge` | `forge.detect` | `ForgeVersionSource`, `ForgePlatformSource` |
| `platforms/common` | `platform.detect` | `DetectionLog` — one log line, called by every loader entrypoint |

`api.detect` depends on `java.*` only. It ships inside the nested `modularity-api` jar, which the
universal jar does not relocate. A loader source lives in its loader's layer root and references only
`api.detect` types and its own loader API.

## 6.2 Version model

`Version` is a sealed base class with two final subtypes:

| Type | Meaning |
|---|---|
| `MinecraftVersion` | A Minecraft release version, `major.minor[.patch]`, e.g. `1.21.1`. Snapshots and pre-releases are rejected. |
| `PlatformVersion` | A loader version, e.g. `0.20.1`, `21.1.265`, `0.20.1+build.42`. Lenient: the numeric prefix fills the parts and the text stays in `raw()`. `unknown()` has no known version. |

The two kinds never compare equal and never compare to each other: `compareTo` throws
`IllegalArgumentException`. A loader version cannot be passed where a Minecraft version is expected.

## 6.3 API

```java
public final class VersionDetector {
    public static MinecraftVersion detect();              // cached; throws VersionDetectionException
    public static Optional<MinecraftVersion> tryDetect();
    public static String source();                        // name of the source that won
    public static void install(VersionSource... sources);// called by the loader entrypoint
}
public interface VersionSource {
    int priority(); String name();
    Optional<MinecraftVersion> detect() throws Exception;
}

public final class PlatformDetector {
    public static DetectedPlatform detect();              // cached; throws PlatformDetectionException
    public static Optional<DetectedPlatform> tryDetect();
    public static String source();
    public static void install(PlatformSource... sources);
}
public interface PlatformSource {
    int priority(); String name();
    Optional<DetectedPlatform> detect() throws Exception;
}
public final class DetectedPlatform {
    static DetectedPlatform of(Loader loader, PlatformVersion loaderVersion, Optional<Side> side,
                               boolean development, String source);
    Loader loader(); PlatformVersion loaderVersion(); Optional<Side> side();
    boolean development(); String source();
}
```

Detection rules:

* A platform installs its sources **before** the first detection. `install` replaces a source with
  the same `name()`, so a repeated call does not duplicate it.
* The detector runs the installed sources and then the built-in sources, by descending `priority`.
  The first result wins and is cached (double-checked `volatile`).
* When every source fails, the exception lists each source with its error.
* There is no `ServiceLoader`. The universal jar copies resources first-wins and relocates class
  names, so a `META-INF/services` file cannot work there.

`DetectedPlatform.loader()` is never `UNKNOWN`. `side()` is empty only when the marker-class source
alone resolved the loader.

Sources in priority order:

| Priority | Source | Where | Result |
|---|---|---|---|
| 1000 | loader source | `loom`, `neoforge`, `forge` | Minecraft version, loader version, side, development flag |
| 100 | `version-json` | `api.detect` | Minecraft version from `version.json` of the game jar |
| 0 | `reflection` | `api.detect` | Minecraft version from `SharedConstants` or `DetectedVersion`; Mojang names only |
| 0 | `marker-classes` | `api.detect` | Loader from a present entry class, Quilt before Fabric, then NeoForge and Forge; no version, no side |

## 6.4 Install points

| Loader | Install point |
|---|---|
| Fabric | `LoomEntrypoints.common("fabric")`, before `ModularityCommon.init` |
| Quilt | `LoomEntrypoints.common("quilt")` — same code path, `LoomPlatformSource` reports `QUILT` |
| NeoForge | `ModularityNeoForgeCommon` constructor; the detection runs in `enqueueWork` |
| Forge | `ModularityForge` constructor |

Each install point then calls `DetectionLog.log()`, which writes one line:

```
(server) platform fabric:0.20.1, minecraft:1.21.1
```

## 6.5 Verification

* Unit tests in `modularity-api`: version parsing, the cross-kind comparison rule, source order,
  error aggregation, `install`, the marker-class order with stub classes on a test class loader.
* Smoke tests for all four loaders check the `(<side>) platform ...` line against
  `gradle/platforms.toml`, and that no separate detector mod is loaded.

## 6.6 History

The detection started as two companion mods, `mvd` (Minecraft Version Detector) and `mpd` (Minecraft
Platform Detector), with eight Gradle projects and their own mod ids. Their code moved into
`modularity-api` and the loader layers; the companion mods and the `companion-mods` table entry are
removed.
