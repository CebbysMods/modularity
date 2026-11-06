# 6. Utility mods: mvd and mpd

Modularity needs to know which Minecraft version and which mod loader it runs on. That detection
lives in two small, independent utility mods that ship next to Modularity:

* **mvd** – Minecraft Version Detector (mod id `mvd`), a clean copy and refactor of
  [minecraft-version-loader](https://github.com/CebbysMods/minecraft-version-loader) (mvl)
* **mpd** – Minecraft Platform Detector (mod id `mpd`), new

The build produces **3 mod jars** (`modularity`, `mvd`, `mpd`). In game they show up as 3 separate
mods. For now both utilities are subprojects of this repository; later they move to their own
repositories.

Scope of the first version: **Fabric, Minecraft 1.21.1, Java 21**. Older versions, NeoForge and
Forge are a later backporting milestone.

## 6.1 Layout

```
utilities/
  mvd/
    gradle.properties          # own group (lv.cebbys.mcmods.mvd) and version (4.0.0)
    mvd-core/                  # plain Java, no Minecraft or loader dependencies
    mvd-fabric/                # Fabric mod jar: fabric.mod.json, entrypoint, Fabric source
  mpd/
    gradle.properties          # own group (lv.cebbys.mcmods.mpd) and version (1.0.0)
    mpd-core/
    mpd-fabric/
```

Rules, so a utility can later be moved out by copying its folder:

* Nothing under `utilities/` references the root project, its `gradle.properties` or any
  `modularity-*` module.
* mvd and mpd do not depend on each other.
* Plain Java, no Lombok or Jakarta annotations.

## 6.2 API

Both utilities have the same shape.

| | mvd | mpd |
|---|---|---|
| Package | `lv.cebbys.mcmods.mvd` | `lv.cebbys.mcmods.mpd` |
| Entry point | `VersionDetector` | `PlatformDetector` |
| Result | `DetectedVersion` | `DetectedPlatform` |
| Detection source (SPI) | `VersionSource` | `PlatformSource` |
| Failure | `VersionDetectionException` | `PlatformDetectionException` |
| Fabric wrapper | `FabricVersionSource`, `MvdFabric` | `FabricPlatformSource`, `MpdFabric` |

```java
// mvd
public final class VersionDetector {
    public static DetectedVersion detect();               // cached; throws VersionDetectionException
    public static Optional<DetectedVersion> tryDetect();
}
public final class DetectedVersion implements Comparable<DetectedVersion> {
    static DetectedVersion parse(String raw, String source); // IllegalArgumentException for snapshots
    int major(); int minor(); int patch(); String raw(); String source();
    boolean isAtLeast(int major, int minor, int patch);
}
public interface VersionSource {
    int priority(); String name();
    Optional<String> detect() throws Exception;           // raw version string
}

// mpd
public final class PlatformDetector {
    public static DetectedPlatform detect();              // cached; throws PlatformDetectionException
    public static Optional<DetectedPlatform> tryDetect();
}
public final class DetectedPlatform {
    static final String UNKNOWN_VERSION = "unknown";
    static DetectedPlatform of(Platform platform, String platformVersion, Side side, boolean development, String source);
    Platform platform(); String platformVersion(); Side side(); boolean development(); String source();
}
public enum Platform { FABRIC, QUILT, FORGE, NEOFORGE, UNKNOWN }
public enum Side { CLIENT, SERVER, UNKNOWN }
public interface PlatformSource {
    int priority(); String name();
    Optional<DetectedPlatform> detect() throws Exception;
}
```

Detection, in both utilities:

* Sources are found with `ServiceLoader` and tried by descending `priority`. The first result wins
  and is cached.
* When every source fails, the exception lists each source with its error (kept from mvl).
* Each mod logs one line at startup, e.g.
  `[mvd] Detected version 1.21.1 (source: fabric-loader)` and
  `[mpd] Detected platform FABRIC 0.16.9, CLIENT (source: fabric-loader)`.

mvd sources, in order:

1. `fabric-loader` (in `mvd-fabric`): version of the `minecraft` mod container.
2. `version-json` (core): `version.json` in the game jar, read through the class loader.
3. `reflection` (core): `SharedConstants` / `DetectedVersion`. Works only where the game is not
   obfuscated (dev environments, Minecraft 26.x), so it comes last.

mpd sources, in order:

1. `fabric-loader` (in `mpd-fabric`): loader version, side and dev flag from `FabricLoader`.
2. `marker-classes` (core): `Class.forName(name, false, loader)` checks, Quilt before Fabric
   (Quilt also contains Fabric's classes), then NeoForge, then Forge. Always returns a result:
   `UNKNOWN` when no marker is found, so `PlatformDetector.detect()` only throws when every source
   throws.

Priorities: loader sources 1000, `version-json` 100, `reflection` and `marker-classes` 0.

## 6.3 What changes from mvl

* Per-version bridges (`mvl-bridge/*`) are removed; detection no longer needs code compiled
  against each Minecraft version.
* `MinecraftVersionUtility` and its hard-coded bridge list are replaced by `VersionDetector` and
  `ServiceLoader`.
* Parsing becomes one regex (`major.minor[.patch]`), the same as `McVersion`. Tests cover both
  `1.21.1` and the year-based `26.1.2`.
* The mod id changes from `minecraft-version-loader` to `mvd`; packages move from
  `lv.cebbys.mcmods.mvl` to `lv.cebbys.mcmods.mvd`.
* The mvl repository is left as it is.

## 6.4 Packaging and integration

* Each utility builds its own mod jar: `mvd-fabric-<version>.jar`, `mpd-fabric-<version>.jar`.
  The `-core` classes are packed into that jar, so each utility is a single file.
* Modularity does **not** nest them (jar-in-jar would show them as children of Modularity in Mod
  Menu). Its `fabric.mod.json` declares `"depends": { "mvd": ">=4.0.0", "mpd": ">=1.0.0" }`.
* `platforms/fabric-1.21.1` uses
  `modImplementation project(path: ':utilities:mvd:mvd-fabric', configuration: 'namedElements')`
  (same for mpd), so `runClient` loads all three as separate mods.
* `FabricRuntime` maps `VersionDetector.detect()` to `McVersion` and
  `PlatformDetector.detect().platform()` to `Loader`. `modularity-api` stays dependency-free.
* A root `collectJars` task copies the 3 jars to `build/libs-all/`.

Fabric metadata of both utilities: `"minecraft": "~1.21.1"`, `"fabricloader": ">=0.16.0"`,
`"java": ">=21"`, no Fabric API dependency.

Bundling details:

* The `-core` jar is packed through a `bundle` configuration (`compileOnly` + `runtimeOnly`), so
  consumers of `namedElements` never get the core classes twice.
* Core and wrapper would both ship `META-INF/services/...Source`. The core's copy is left out of
  the mod jar; the wrapper's file lists the core sources too (`ServiceLoader` ignores the duplicate
  entries in development, where both are on the class path).
* `mvd` has its own icon (`assets/mvd/icon.png`, 256x256); `mpd` has no icon yet.

## 6.5 Verification

* JUnit tests in `mvd-core` (parsing, source order, error aggregation) and `mpd-core` (marker
  class order with fake classes on a test class loader, `UNKNOWN`).
* `tools/smoke-fabric-1.21.1` installs the 3 jars on server and client and checks the log for
  `modularity`, `mvd` and `mpd` as separate loaded mods, mvd reporting `1.21.1` and mpd reporting
  `FABRIC`. `SmokeServer` also logs each of the three mods with `nested=<parent>|no`.
  The utilities are published to Maven local (`./gradlew publishToMavenLocal`) and the smoke build
  adds them with `modRuntimeOnly`, like Modularity.

Result of the local run (server, then client):

```
(FabricLoader) Loading 46 mods:
	- modularity 0.1.0-SNAPSHOT
	- mpd 1.0.0
	- mvd 4.0.0
[smoke] mod modularity: loaded=true version=0.1.0-SNAPSHOT nested=no
[smoke] mod mvd: loaded=true version=4.0.0 nested=no
[smoke] mod mpd: loaded=true version=1.0.0 nested=no
[mvd] Detected version 1.21.1 (source: fabric-loader)
[mpd] Detected platform FABRIC 0.16.9, SERVER (development) (source: fabric-loader)
Modularity 0.1.0-SNAPSHOT on Fabric, Minecraft 1.21.1: initializing 0 mod(s)
...
[mpd] Detected platform FABRIC 0.16.9, CLIENT (development) (source: fabric-loader)
```

## 6.6 Deviations from the plan

* `Side` has a third value, `UNKNOWN`, and `DetectedPlatform.UNKNOWN_VERSION` exists: the
  loader independent marker-class source cannot know the side or the loader version.
* `DetectedVersion.parse` and `DetectedPlatform.of` are public, so sources outside the core
  (the Fabric wrappers, future loaders) can build results.
* Gradle 8.14 does not run on JDK 25; run it with a JDK 21 `JAVA_HOME`.

## 6.7 Steps

Branch `feature/mvd-mpd-utilities`, one commit per step:

1. Gradle skeleton: `utilities/` folders, settings includes.
2. `mvd-core`: clean copy and refactor of mvl, sources, tests.
3. `mvd-fabric`: mod wrapper and Fabric source.
4. `mpd-core` and tests.
5. `mpd-fabric`.
6. Modularity: dependencies, `depends`, `FabricRuntime` using the detectors.
7. Smoke test and `collectJars`.
8. Docs update.

## 6.8 Later

* Backports to older Minecraft versions (Java 17 cores, `version.json` and loader metadata for
  obfuscated releases).
* NeoForge and Forge wrappers, or one jar per utility carrying both `fabric.mod.json` and
  `neoforge.mods.toml`.
* Move mvd and mpd to their own repositories.
