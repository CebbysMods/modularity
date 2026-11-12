# 6. Runtime detection

Modularity must know which Minecraft version and which mod loader it runs on. The detection lives in
Modularity itself, in `modularity-api`; no separate detector mod is needed.

## 6.1 Where the code lives

| Layer | Package | Content |
|---|---|---|
| `modularity-api` | `api.core` | `Version`, `MinecraftVersion`, `PlatformVersion`, `Loader`, `Side` |
| `modularity-api` | `api.detect` | `VersionDetector`, `PlatformDetector`, the source interfaces, `DetectedPlatform`, the built-in sources and the exceptions |
| `platforms/loom` | `loom.detect` | `ModularityLoomPlatformSource`, `ModularityLoomVersionSource` — the Fabric Loader calls both Loom loaders share; they implement `detect()` |
| `platforms/fabric`, `platforms/quilt` | `fabric.detect`, `quilt.detect` | `ModularityFabricPlatformSource`/`ModularityFabricVersionSource`, `ModularityQuiltPlatformSource`/`ModularityQuiltVersionSource` — the loader, the source name and the version container |
| `platforms/anvil` | `anvil.detect` | `ModularityAnvilPlatformSource`, `ModularityAnvilVersionSource` — the `DetectedPlatform` assembly and the source name both Forge-family loaders share; they name no loader type |
| `platforms/neoforge`, `platforms/forge` | `neoforge.detect`, `forge.detect` | `ModularityNeoForgePlatformSource`/`ModularityNeoForgeVersionSource`, `ModularityForgePlatformSource`/`ModularityForgeVersionSource` — each supplies the values its own FML reports |
| `platforms/common` | `platform.detect` | `AbstractModularityPlatformSource` — the loader priority and the `detect()` assembly; `AbstractModularityVersionSource` — the loader priority and the version parse; `DetectionLog` — one log line, called by every loader entrypoint |

`api.detect` depends on `java.*` only. It ships inside the nested `modularity-api` jar, which the
universal jar does not relocate. A loader source lives in its loader's layer root and references only
`api.detect` types and its own loader API.

`AbstractModularityPlatformSource` assembles `detect()`; a source answers loader, version, side and
development from its own loader API. Fabric and Quilt share the calls into the Fabric Loader API, so
`ModularityLoomPlatformSource` and `ModularityLoomVersionSource` hold them; every `loom` source adds `getLoader()`,
and the platform source also adds `getModContainerName()`. NeoForge and Forge share no type, so their four sources extend the `anvil`
bases, which hold only the source name, and read their own FML.

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
| 1000 | loader source | `loom` base with the child in `fabric`/`quilt`, or the child in `neoforge`/`forge` | Minecraft version, loader version, side, development flag |
| 100 | `version-json` | `api.detect` | Minecraft version from `version.json` of the game jar |
| 0 | `reflection` | `api.detect` | Minecraft version from `SharedConstants` or `DetectedVersion`; Mojang names only |
| 0 | `marker-classes` | `api.detect` | Loader from a present entry class, Quilt before Fabric, then NeoForge and Forge; no version, no side |

## 6.4 Install points

| Loader | Install point |
|---|---|
| Fabric | `ModularityLoomCommon.init(Loader.FABRIC, new ModularityFabricPlatformSource(), new ModularityFabricVersionSource())`, before `ModularityCommon.init`; the launch stage passes the same sources |
| Quilt | `ModularityLoomCommon.init(Loader.QUILT, new ModularityQuiltPlatformSource(), new ModularityQuiltVersionSource())` — same code path; the Quilt sources report `quilt` |
| NeoForge | `ModularityNeoForgeCommon` constructor, through `ModularityAnvilServices` with `ModularityNeoForgePlatformSource` and `ModularityNeoForgeVersionSource` |
| Forge | `ModularityForge` constructor, through `ModularityAnvilServices` with `ModularityForgePlatformSource` and `ModularityForgeVersionSource` |

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
