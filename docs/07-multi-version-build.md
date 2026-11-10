# 7. Multi-version build (Fabric)

Modularity ships one Fabric platform jar per Minecraft version, from 1.21.1 to the latest stable
release. The code is written once and layered: Fabric code common to every version, then only the
changes per version range, then the few details of a single version. Our own Gradle plugin
(`lv.cebbys.modularity`, in `build-logic/`) assembles every platform from these layers.

## 7.1 Versions

Source of truth: `gradle/platforms.toml`. Values come from Fabric's metadata
(`meta.fabricmc.net/v2/versions/game`, `.../loader`), Fabric's Maven (`fabric-api` metadata, newest
build per exact Minecraft version) and Mojang's version manifest (`javaVersion.majorVersion`).
Checked on 2026-11-06.

| Platform | Minecraft | Fabric API | Java | Loom plugin |
|---|---|---|---|---|
| fabric-21.1 | 1.21.1 | 0.116.17+1.21.1 | 21 | remap (Mojang mappings) |
| fabric-21.2 | 1.21.2 | 0.106.1+1.21.2 | 21 | remap |
| fabric-21.3 | 1.21.3 | 0.114.1+1.21.3 | 21 | remap |
| fabric-21.4 | 1.21.4 | 0.119.4+1.21.4 | 21 | remap |
| fabric-21.5 | 1.21.5 | 0.128.2+1.21.5 | 21 | remap |
| fabric-21.6 | 1.21.6 | 0.128.2+1.21.6 | 21 | remap |
| fabric-21.7 | 1.21.7 | 0.129.0+1.21.7 | 21 | remap |
| fabric-21.8 | 1.21.8 | 0.136.1+1.21.8 | 21 | remap |
| fabric-21.9 | 1.21.9 | 0.134.1+1.21.9 | 21 | remap |
| fabric-21.10 | 1.21.10 | 0.138.4+1.21.10 | 21 | remap |
| fabric-21.11 | 1.21.11 | 0.141.6+1.21.11 | 21 | remap |
| fabric-26.1 | 26.1 | 0.145.1+26.1 | 25 | no remap (unobfuscated) |
| fabric-26.1.1 | 26.1.1 | 0.145.4+26.1.1 | 25 | no remap |
| fabric-26.1.2 | 26.1.2 | 0.155.3+26.1.2 | 25 | no remap |
| fabric-26.2 | 26.2 | 0.161.0+26.2 | 25 | no remap |
| fabric-26.3 | 26.3 | 0.165.2+26.3 | 25 | no remap |

Fabric Loader: 0.20.1 (latest stable) for every platform.

## 7.2 Layout

```
gradle/platforms.toml                      version table: one entry per loader and Minecraft version
build-logic/                               included build: the lv.cebbys.modularity plugins
platforms/
  minecraft/                               plain Minecraft code shared by every loader (see 7.3), package
                                           lv.cebbys.mcmods.modularity.platform: runtime, blocks, entities,
                                           client rendering, mixins, and the Minecraft-facing loader seams
  common/                                  code shared by every loader that does not belong to Minecraft:
                                           the entrypoint roles, the mod lifecycle, the loader seam registry
                                           (platform.loader.Loaders) and the guest descriptor
  loom/                                    Fabric-API code shared by Fabric and Quilt
  fabric/ | neoforge/ | forge/ | quilt/    one layer root per loader, each with:
    common/src/main/{java,resources}         code shared by every version of the loader
    changes/<range>/src/main/{java,resources} one change, at the first version that needs it
    versions/<version>/                      project dir of platform <loader>-<version>;
      src/main/{java,resources}              only truly single-version details
tools/smoke/minecraft | loom | fabric | quilt | neoforge | forge
                                           smoke-test mods (same layering, see 7.6); tools/smoke/guest
```

Range folder names: `1.21.5+` (that version and every later one) or `1.21.5-1.21.8` (inclusive).

Platforms are `<loader>-<version>` (`fabric-21.1`, `neoforge-21.1`) as selection ids; their projects are
`:platforms:<version>:<version>-<loader>` (`:platforms:21.1:21.1-fabric`), grouped under one parent per
Minecraft version.
`-Pmodularity.platforms=fabric-21.1,neoforge-21.1` selects platforms by id, a bare version (`21.1`) every
loader's platform of that version.

Each loader assembles a stack of layer roots, lowest first:

| Loader | Layer roots | Own jar |
|---|---|---|
| fabric | `minecraft`, `common`, `loom`, `fabric` | yes |
| quilt | `minecraft`, `common`, `loom`, `quilt` | yes |
| neoforge | `minecraft`, `common`, `neoforge` (`anvil` later) | yes |
| forge | `minecraft`, `common`, `forge` (`anvil` later) | yes |

`minecraft` holds Minecraft code; `common` holds code that every loader shares and that does not belong
to Minecraft (entrypoint roles, lifecycle, seam registry, guest descriptor); `loom` holds the Fabric API
code that runs on both Fabric and Quilt Loader. The future Forge-family root will be `anvil`
(`minecraft`, `common`, `anvil`, `neoforge` / `forge`).

Code that needs a loader API goes through the seams in `platform.loader`, installed by the loader's entrypoint
through `Loaders.install` (client: `installClient`):

| Seam | Covers | Loom implementation | NeoForge implementation |
|---|---|---|---|
| `LoaderInfo` | side, loaded mods and versions, development mode, mods with a `modularity` entrypoint | `LoomLoaderInfo` | `NeoForgeLoaderInfo` |
| `RenderDataAccess` | block entity render data for models, also off the game thread | `LoomRenderData` | `NeoForgeRenderData` |
| `StorageBackend` | fluid unit, exposing entity tanks and inventories, filling from a held item | `LoomStorageBackend` | `NeoForgeStorageBackend` |
| `LoaderHooks` / `ClientHooks` | registration, block entity types, creative tabs, payloads; key mappings, entity renderers, dynamic block models, HUD, reload, client tick | `LoomLoaderHooks` / `LoomClientHooks` | `NeoForgeLoaderHooks` / `NeoForgeClientHooks` |

The NeoForge seam is wired for the lifecycle only: content registration, payloads, render data and storage
fail with an `UnsupportedOperationException` that names the method, and dynamic block models and HUD layers
log a warning when a mod passes a non-empty collection. NeoForge constructs the mod entry points as three
`@Mod` classes — common, `Dist.CLIENT` and `Dist.DEDICATED_SERVER` — so a dedicated server never loads client
types. Each class registers its setup event and runs the init in `enqueueWork`.

A higher root may replace a shared file at the same path (`loom` does for `SyncedData` from 1.21.9 and for
`RenderDataHolder`). The Quilt entrypoints implement the same Fabric interfaces for `main`, `client` and
`server`, and native Quilt `pre_launch` for the launch role.

## 7.3 Layering rules

For platform version V the sources of each layer root are, lowest to highest (roots stack in the order of 7.2):

1. `common`
2. every `changes/<range>` that contains V, ordered by range start (then by range end, open last)
3. `versions/<V>`

A file in a higher layer replaces the file with the same relative path below it. A file without a
counterpart below is an addition. A file `X.removed` (empty) removes `X` coming from below.

The plugin fails the build when:

* a range folder name cannot be parsed, or two ranges are identical;
* a layer contains a file identical to the one it replaces (a copy instead of a change);
* a `.removed` marker names a file that does not exist below.

Rules for authors: put a change at the first version that needs it, as an open range (`X+`). Close a
range (`X-Y`) only when a later version changes the same file again, and then add `Y'+` for the new
form. Never copy common code into a version folder.

## 7.4 Plugin responsibilities

Two plugins in `build-logic/`:

* `lv.cebbys.modularity.settings` (settings plugin): reads `gradle/platforms.toml`, includes one parent
  project per Minecraft version, `:platforms:<version>` (dir `platforms/versions/<version>`), and one
  project per platform, `:platforms:<version>:<version>-<loader>` (dir
  `platforms/<loader>/versions/<version>`). Each parent gets `compile` (the children's `compileJava`),
  `build` (classes and tests of every loader, then `jar`), `clean` and `smoke`, plus `genSources` and `jar`
  as the individual steps, aggregating its loader projects; a child without the task is skipped.
  `genSources` decompiles once for the version (the Fabric platform; see the decision on the shared
  `loom-cache` below), since the mappings are the same for every loader, and it is **not** part of `build`:
  Gradle deletes the shared Maven directory before decompiling, which breaks any other Loom project reading
  it in the same parallel run. Run it on its own after a Minecraft or mappings change.
* `lv.cebbys.modularity` (project plugin), applied to each platform project: reads the table and hands the
  project to its loader's setup in `lv.cebbys.modularity.gradle.setup` — `FabricPlatformSetup` and
  `QuiltPlatformSetup` over the shared Loom base, `NeoForgePlatformSetup` and `ForgePlatformSetup` over the
  shared Forge-family base. Every setup runs the same stages in the same order (loader plugins, Java setup,
  layered sources, dependencies, tasks, runs, publication); the bases own the stages the loaders share and
  each loader fills in only its own:
  * applies the loader's Gradle plugin (Loom, `net.fabricmc.fabric-loom-remap` with Mojang mappings up to
    1.21.11 and `net.fabricmc.fabric-loom` without mappings from 26.1, ModDevGradle or ForgeGradle) and the
    Java target of the version;
  * adds Parchment over the official mappings where `[parchment]` in `gradle/platforms.toml` has a version
    for the Minecraft version (Loom `layered`, ModDevGradle `neoForge.parchment`, ForgeGradle
    `minecraft.mappings("parchment", …)`), from
    `https://maven.parchmentmc.org`. Parchment is
    compile-time metadata (parameter names, javadoc): class and member names, the remapped jar and the runtime
    do not change, so the universal jar is the same with or without it. A version without a release builds
    with the official mappings alone. On Forge the mappings request must also be pinned as attributes on the
    Minecraft dependency: ForgeGradle's run launcher resolves its class path through an attribute-less
    detached configuration that project configuration hooks cannot reach, so with the Parchment variants
    published next to the official ones it fails with "Cannot choose between the available variants" unless
    the dependency itself asks for the Parchment channel;
  * assembles the layers into `build/layered/` (task `assembleLayers`) and compiles from there;
  * adds IDE-only source sets `ideLayers0…` to each platform project: one per layer root, with the files
    that a higher layer replaces or removes excluded, so IntelliJ indexes the layer folders themselves.
    These source sets are never compiled (their compile and resources tasks are disabled) and nothing
    depends on them;
  * generates the loader metadata (Fabric: `fabric.mod.json`; Quilt: `quilt.mod.json`; NeoForge/Forge:
    the TOML file) from the table and the assembled sources. It declares the exact Minecraft version and
    the table's dependencies, and the mixin config from the mixin classes that are actually present after
    layering. Entrypoints are every class that directly implements a Fabric entrypoint interface; the
    build calls the roles `common`, `client`, `server` and `launch`, and maps them to the loader's keys:
    Fabric `main`, `client`, `server`, `preLaunch`; Quilt `main`, `client`, `server`, `pre_launch`. The
    Quilt metadata also carries the Maven group, `intermediate_mappings: net.fabricmc:intermediary`; the
    `quilt_loader.jars` list is added to the built jar after Loom nests the modules (`QuiltJarMetadata`),
    because the development resources must not name a file that the jar task has not created yet. Each
    loader's mixin config has its own name (`modularity.mixins.json`, `modularity.quilt.mixins.json`),
    because the universal jar carries them side by side;
  * dependencies: the loader (Fabric Loader, Quilt Loader, NeoForge or Forge), Fabric API on Fabric and
    Quilt, `modularity-api` (nested);
  * tasks per platform: `compile` (the Java plugin's `compileJava`), `runClient`, `runServer`, `smokeServer`,
    `smokeClient`;
  * root tasks: `buildAll`, `collectJars` (Sync of the universal jars into `build/libs-all`), `smokeAll`.

### The universal jar

The parent's `jar` task (`UniversalJarTask`) writes the one artifact of a version,
`build/libs/modularity-<minecraft>-<modversion>.jar` (`<archive-prefix>-<minecraft>-<version>`, the first
two from `gradle/platforms.toml`), loadable by Fabric, Quilt, Forge and NeoForge. The per-loader jars stay
build intermediates and are not published.

Fabric/Quilt are remapped to intermediary, Forge/NeoForge run with Mojang names, so one compiled class set
cannot serve both: the universal jar carries **one class set per loader**, each moved under
`lv.cebbys.mcmods.modularity.universal.<loader>` with ASM `ClassRemapper`, and every class reference inside
it follows. The metadata file of each Loom loader (Fabric `fabric.mod.json`, Quilt `quilt.mod.json`) and that
loader's mixin config (`package` and `mixin` name) are rewritten to the relocated tree. Metadata stays one
file per loader (`fabric.mod.json`, `quilt.mod.json`, `META-INF/mods.toml`, `META-INF/neoforge.mods.toml`),
the Fabric API and `modularity-api` nested jars are copied as they are, and shared resources (the icon) are
stored once.

Quilt contributes its own class set, its own entrypoints (`ModularityQuiltCommon`, `...Client`,
`...Server` over the Fabric interfaces, and `...Launch` over Quilt's `PreLaunchEntrypoint`), its own
metadata and its own mixin config. The Mixin refmap and Forge/NeoForge mixins, which make the jar
production-ready, are beans `modularity-dals` and `modularity-vk1z`.

## 7.5 Runtime detection

Modularity detects the Minecraft version and the loader itself (docs/06). `api.detect` holds the
detector, the source interface and the built-in sources; each loader layer holds its own source and
installs it in the entrypoint. The detector runs the installed sources first and the built-in ones
after them, and caches the first result. Each install point logs one line,
`[modularity] platform <LOADER> <loaderVersion>, Minecraft <version>, <SIDE>`.

There is no `META-INF/services` file: the universal jar copies resources first-wins and relocates
class names, so explicit `install(...)` replaces `ServiceLoader`.

## 7.6 Smoke tests

The smoke mod lives in `tools/smoke/` and is layered like the platform. Each platform project gets a
`smoke` source set and two runs:

* `smokeServer`: dedicated server, flat world, stops itself after a few ticks; the task then checks
  the log: Modularity loaded as a top-level mod, the detection line naming the exact version and
  the loader (`FABRIC`, `QUILT`, `NEOFORGE`, `FORGE`).
* `smokeClient`: client joins the world written by the server run, takes a screenshot, quits; same
  log checks.

The shared smoke sources live in `tools/smoke/minecraft`, `tools/smoke/loom` and the loader roots; a root
without a `tools/smoke/<root>` folder is skipped. The smoke guest mod loads on Fabric only (docs/08), so
the Quilt check expects no guest lines.

Every platform runs `smokeServer`; `smokeClient` runs for 21.1, 21.6, 21.11, 26.1 and the latest.

CI runs each platform in its own workflow job, in parallel. A discover job reads the enabled platforms
from `gradle/platforms.toml`, so the matrix never drifts from the table. A platform with
`smoke-client = true` runs `smoke` under `xvfb` with software OpenGL. A platform without it runs only
`smokeServer`, with no virtual display.

## 7.7 Risks

* One Loom version for the whole build: Loom 1.19 must build every version (it does: both plugins
  ship in one artifact; the mixed obfuscated/unobfuscated build bug, fabric-loom#1477, was fixed in
  1.15).
* Gradle must run on Java 25 for 26.x; 1.21.x targets Java 21 bytecode via toolchains.
* Big API jumps (1.21.2 items/recipes/render state, 1.21.4 item models, 1.21.5 data components and
  rendering, 1.21.6 HUD and rendering, 1.21.9, 26.1 names and render pipeline) need real porting
  work, not just recompilation.
* Compiling from `build/layered/` means compiler errors point at the layered copy; the error message
  includes the layer the file came from (`assembleLayers` writes `build/layered/origins.txt`).

## 7.8 Decisions

| Decision | Reason |
|---|---|
| Own plugin instead of Stonecutter | Matches the layered model (common + changes) instead of `//? if` comments; can grow to other loaders and to mods built on Modularity. |
| Loom 1.19.3, Gradle 9.8.2, Gradle on JDK 25 | Newest stable Loom; ships both the remap and the non-remap plugin; 26.x requires Gradle on Java 25. |
| Fabric Loader 0.20.1 everywhere | Latest stable; supports every version in the table, so one loader version keeps the table simpler. |
| Newest Fabric API build per exact version | Most fixes; Fabric API builds are tied to one Minecraft version. |
| Layers assembled into `build/layered/` | Java has no per-file source override; copying the winning file of each path is simple, cacheable and keeps the source layers clean. |
| Additions allowed, only removals must point at an existing file | On disk an override and an addition look the same; requiring every file to exist below would forbid new classes in newer versions. |
| Identical copies fail the build | Enforces "only the changes" mechanically. |
| Project dir = `platforms/<loader>/versions/<version>`, grouped under `:platforms:<version>` | One folder per loader and version, created by the settings plugin; the parent project aggregates them. |
| Loom loaded once in the root `plugins {}` (`apply false`), subprojects apply it without a version | One Loom classloader for the whole build; separate versions per project break Loom's shared services. |
| Utility folders pass their `gradle.properties` values to subprojects explicitly | Gradle 9 deprecates the implicit lookup in parent projects (an error in Gradle 10). |
| refabslab integration smoke (`tools/smoke-fabric-1.21.1`) is local only | It builds CebbysMods/refabslab first, which is not a Modularity check; CI runs only the platforms of the table. |
| Three plugins: `.settings` (includes projects), `.root` (applies the platform plugin to every included platform, `buildAll`, `collectJars`, `smokeAll`), `lv.cebbys.modularity` (one platform) | Platform projects need no build file at all; the version folder only holds sources. |
| Mod metadata, nested module and runtime mods in `[mod]` of `platforms.toml` | The plugin stays free of Modularity paths and can later build mods on Modularity. |
| `enabled = false` per table entry until the version is ported; `-Pmodularity.platforms=21.1,26.3` selects platforms | All versions are recorded once; ports switch one flag; fast builds of a few platforms while porting. |
| Own small TOML reader (`MiniToml`) | The table uses a tiny subset; no extra plugin dependency or Gradle internal API. |
| Final smoke run one platform per Gradle call (daemon stopped in between) instead of one `smokeAll` | On the 32 GB dev machine a single `smokeAll` was killed for low system memory; the per-platform `smoke` tasks are the same checks. CI runs one job per platform, each with one platform per Gradle call. |
| Mod Menu in every platform's runs (`modmenu` per table entry, development only) | Shows the loaded mods, guest mods included, in every version; newest Mod Menu release per Minecraft version from Modrinth, checked on the Terraformers maven. |
| Platforms keyed by loader and version (`fabric-21.1`); shared roots `minecraft` (Minecraft code) and `common` (non-Minecraft code), then the loader family (`loom`) and the loader | Most code is plain Minecraft code; version changes in it are made once for every loader. Code that is shared but names no Minecraft type (`EntryPoint`, the mod lifecycle, `Loaders`, `GuestDescriptor`) lives in `common`, so the split follows what a file depends on, not which loader happens to use it first. |
| `loom` root for Fabric and Quilt; `anvil` reserved for Forge and NeoForge | Fabric API code that both Fabric and Quilt Loader run needs one home; the Forge-family counterpart gets `anvil` when that work starts. |
| Quilt has its own class set, `quilt.mod.json`, entrypoints and mixin config in the universal jar | Full loader separation: Quilt Loader reads native metadata, not Fabric's; the universal jar carries `fabric.mod.json` and `quilt.mod.json` side by side. Quilt Loader 0.30.2 dispatches `main`/`client`/`server` to `ModInitializer`/`ClientModInitializer`/`DedicatedServerModInitializer` and `pre_launch` to `PreLaunchEntrypoint` (checked in the loader's sources), so the Quilt entrypoints reuse the same interfaces for the first three roles. |
| Quilt uses Fabric API as its library; QSL is not used | QSL for 1.21.1 is alpha-only and discontinued, and it lacks transfer/fluid, item and entity lookups, client events and model loading. Quilt Loader provides the Fabric Loader API, so `loom` compiles once. |
| The universal jar carries one full class set per loader, Quilt included | Real separation costs the duplicated shared `common`/`loom`/`minecraft` classes. Sharing the Fabric tree instead would leak Quilt overrides into Fabric. |
| A native `quilt.mod.json` is generated only when the platform is remapped (Fabric/Quilt on an obfuscated version) | The QMJ names `net.fabricmc:intermediary`; a 26.x Loom platform uses Mojang names, so the metadata task fails with a clear message until the Mojang-mapping path is designed. |
| `quilt_loader.jars` is added to the built jar, not to the generated resources | Quilt Loader in development reads the metadata from `build/resources/main` and opens every nested jar it names; Loom creates the nested jars only in the jar task. Loom does the same for `fabric.mod.json`; `QuiltJarMetadata` does it for the QMJ after the jar is built. |
| Quilt through Fabric Loom with Quilt Loader as the loader dependency, no Quilt Loom | One Loom for the whole build. Loom reads loader libraries only from `fabric-installer.json`; the plugin adds the `common` and `development` libraries of Quilt Loader's own `quilt_installer.json`, copies Loom's `fabric.*` development properties in `launch.cfg` to Quilt's `loader.*` names, and sets Quilt's `KnotClient`/`KnotServer` main classes. |
| Quilt platforms compile against Fabric Loader (compile only) and exclude it at runtime | `loom` uses the Fabric Loader API (`LoomLoaderInfo`, `GuestCandidates`), which Quilt Loader provides; at runtime only Quilt Loader may be present (both ship `net.fabricmc.loader.impl`). |
| `fabricloader >=0.16.0` in the Fabric jars (`fabric-loader-min`), built and run with 0.20.1 | Quilt Loader 0.30.2 reports Fabric Loader 0.19.5, so `>=0.20.1` refused the Fabric jar on Quilt. The loader API Modularity uses is old and stable. Only guest mods depend on 0.20.1 internals (docs/08). |
| Quilt, NeoForge and Forge only for 1.21.1 for now | Scope set by the project owner. Quilt was also tried on every other version: 1.21.2 to 1.21.11 passed on Quilt Loader 0.30.2; 26.x needs Quilt Loader 0.31.0-beta.6 (0.30.2 loads the mods there but never calls their entrypoints) and then passed too. |
| No guest mods on Quilt yet | The guest loader uses Fabric Loader internals that Quilt Loader does not have; on Quilt it logs an error and loads no guests. Quilt smoke tests run without the guest. |
| NeoForge with ModDevGradle 2.0.148, Forge with ForgeGradle 7.0.40, both on the shared plugin classpath next to Loom | The plugins and versions of NeoForge's and Forge's own 1.21.1 MDKs, both on Gradle 9. ModDevGradle's legacy Forge plugin cannot build Forge 1.21.1 and ForgeGradle 6 needs Gradle 8. All three plugins in one build were tried first in a throwaway build. |
| NeoForge/Forge metadata (`neoforge.mods.toml`, `mods.toml`) generated from the same `[mod]` values; Minecraft exact, the loader from its line on (`[21.1,)`, `[52,)`), `>=X` ranges as `[X,)` | One description of the mod for every loader. |
| `[mod] mods` lists the Fabric modules; NeoForge and Forge use their `-neoforge` / `-forge` sibling | The separate mods are the same mods per loader. |
| Smoke mods layered like the platforms (`tools/smoke/minecraft` + the loader's root) | The screenshot code is plain Minecraft code and its version changes apply to every loader. |
| NeoForge smoke source set: own configurations extending `main`'s, plus `addModdingDependenciesTo` | ModDevGradle adds its dev launcher to the source set's configurations; a plain file class path misses it. |
| Forge runs named `server` / `client`, smoke tasks `runSmokeServer` / `runSmokeClient` | ForgeGradle 7 fills a run from Forge's run configuration of the same name and registers one task per source set (`run<SourceSet><Name>`) with that source set's class path; a run's `classpath` option replaces the launcher's own class path. |
| Forge: resources compiled into the classes folder (`main`, `smoke`) | Forge's development discovery takes each class path folder as a mod file; `mods.toml` must sit next to the classes. |
| Forge jar-in-jar written by the plugin (`META-INF/jarjar` + `metadata.json`, same format as ModDevGradle) | ForgeGradle 7 has no jarJar; Forge 52 reads that format (`JarInJarDependencyLocator`). Loading the nested API from a production jar is still to be checked in a real Forge install. |
| `pack.mcmeta` (pack format 34) in the Forge layer, smoke mod and utility mods | Forge 1.21.1 warns for mods without it; the value is the one of Forge's own 1.21.1 MDK. |
| NeoForge/Forge utility mods: the bundled core is not also a runtime dependency | Both load mods as Java modules; the core's package may come from one module only (Forge refused the split package). |
| Forge smoke "nested" check by jar path (`META-INF/jarjar/`) | Forge's mod file API names no parent file (NeoForge's does). |
| `build-logic` compiled for Java 25 | Loom 1.19.3 is published as Java 25 bytecode, so plugins depending on it must be too. |
| Mixin config `compatibilityLevel` JAVA_21 for every platform | The mixins use no newer language features; one value every shipped Mixin knows. |
| Mixins listed from `@Mixin` classes in the assembled sources, `client.*` under `client` | A version layer can add or remove a mixin without editing a config. |
| Mixin names sorted as strings | Windows sorts paths case-insensitively; the generated config must be identical on every OS. |
| 21.1 move verified by jar diff | Every class in the 21.1 jar is byte-identical before and after the move; only `fabric.mod.json` (exact `minecraft`, `fabricloader >=0.20.1`) and the order of two client mixins changed. |
| `assembleLayers` inputs are only `*/src/<set>/**` of the layers | `versions/<v>` is also the project dir; its `build/` and `run/` must not be inputs. |
| Smoke tests built before the ports (goal step 7 moved before step 5) | Every port commit is verified with the same `smoke` task for that version and all earlier ones. |
| Smoke mod as source set `smoke` of each platform, own Loom runs in `build/smoke/run` | Same classpath and mappings as the platform; no second build, no maven round trip; each source set is its own mod (Loom `mods {}`). |
| Fresh flat world per server run; the client opens that world with `--quickPlaySingleplayer` | The client test needs no world creation UI; the world matches the version. |
| Log checks in `SmokeCheck` (unit tested): Modularity loaded and not nested, `[modularity] platform <LOADER> <loaderVersion>, Minecraft <exact>, <SIDE>`, end marker, no crash or failed mixin | The goal's verification, done mechanically; matched lines go to `build/smoke/{server,client}.txt`. |
| `tools/smoke-fabric-1.21.1` kept | It is the refabslab integration test (double slabs, KleeSlabs), separate from the platform smoke tests. |
| One decompilation per version, by the Fabric platform (the parent's `genSources` points there); it is not part of `build`, and the other platforms' tasks `mustRunAfter` it | Loom keeps one mapped Minecraft jar and one Maven directory per version in the root `loom-cache` and declares the directory an output of `genSourcesWithVineflower`; Gradle deletes declared outputs before running a task. Decompiling inside a parallel build therefore deletes files the other platforms of the version read, so they wait for it, and `build` leaves decompilation to an explicit `genSources` run. Sources after compilation is impossible (Loom's `genSources` depends on its own `remapJar`). The mappings are the same for every loader of a version, so one decompilation serves them all. No platform source depends on the decompiled output, so `genSources` is run on its own. |
| Parchment layered over the official mappings, one entry per Minecraft version in `[parchment]` | Parameter names and javadoc in the Minecraft classes the platforms compile against, without changing names, remapping or runtime: the universal jar is byte-identical. It is published per Minecraft version, not per loader, so one entry covers Fabric, Quilt, NeoForge and Forge; versions without a release (1.21.2, 26.x) build with the official mappings alone. Shape: Loom `layered` for Fabric/Quilt, ModDevGradle `neoForge.parchment` for NeoForge, ForgeGradle `minecraft.mappings("parchment", …)` for Forge. ForgeGradle's run launcher resolves the run class path in an attribute-less detached configuration that no project configuration hook can reach, so on Forge the mappings channel and version are additionally set as attributes on the Minecraft dependency; that is what makes the two published variants of `net.minecraftforge:forge` resolvable there. |
| CI: one parallel job per enabled platform, then a final job; smoke jobs get JDK 8, 21 and 25 and run under xvfb for client runs | Per-platform results stay visible and the jobs run at the same time. The matrix comes from `gradle/platforms.toml`, so the table stays the only source. A per-platform job cannot make the universal jar, which merges every loader of a version, so the final job runs `collectJars` with every platform. Loom 1.19 needs Gradle on 25; 1.21.x toolchains need 21; ForgeGradle's slime launcher needs 8. setup-java writes all three to `~/.m2/toolchains.xml`, where Gradle finds them. |
| Detection code compiled into `modularity-api` at release 17 | It uses no Minecraft and no loader class; the loader sources live in the loader layers. |
| Version calls behind small classes in `loom/compat` (seams), added to the `common` layer in their own commits before the port that needs them | A layer then replaces a 20-line seam instead of a large file, so "only changes per version" holds and big classes stay in the `common` layer once. |
| Entity base as a chain, one concern per class (`SavedEntity` ← `DamageEntity` ← `UsableEntity` ← `SolidEntity` ← `BoundsEntity` ← `InterpolatedEntity`) | Saving, damage, use, collision, bounds and interpolation changed in different versions; one class per concern means one replaced file per change, without copying the others. |
| `.removed` markers used for classes a version no longer needs (1.21.9 `BufferPoseOutput`, `ParticleEngineMixin`; 26.2 `EntityAccessor`) | A leftover mixin targeting a missing member fails at run time; removing it is the change. |
| Open ranges stack (`1.21.2+`, `1.21.4+`, … `26.3+` all apply to 26.3); no closed range was needed | Each change starts where Minecraft changed and stays; a file changed twice simply has the newer copy in the later range. |
| Client smoke run for every newly ported version, not only the five in the table | Two mixin targets (1.21.9 `ParticleEngine#crack`, 26.1 `Camera#setup`) compiled but failed at run time; only a client start shows it. |
| Remap tasks limited to 2 at a time (`RemapService`), daemon heap 4 GB | A cold `buildAll` of 11 obfuscated versions in parallel ran out of heap; the limit keeps the build parallel elsewhere. |
| 26.2 part ids reserved from `ServerLevel`'s counter, only on the server | The counter moved from `Entity` to `ServerLevel` (which skips ids in use); client entities take their id from the spawn packet. |
| One icon for every loader: `[mod] icon` in `platforms.toml`, copied to `assets/<id>/icon.png` | Generated with the metadata, so Fabric (`icon`) and NeoForge/Forge (`logoFile`) point at the same file in every jar; the 256×256 source is `images/modularity.png`. |
| Shared helpers instead of copies in the 1.21.1 base (`GameCompat.result`, `NbtData.encode/decode/writeAll/readAll`, `ModularityCommon.forEachMod`, handle records, `PartShape.slab`, `Props.with`) | Each copy existed once per caller; one helper is one place to fix and one place for the later shared Minecraft layer. Tolerances and error texts kept, so no behaviour change. |
| Loader seams `LoaderInfo`, `RenderDataAccess`, `StorageBackend`, `LoaderHooks` + `ClientHooks`, installed through `Loaders` | Everything the shared code needs from a loader goes through four interfaces; a new loader implements them (Fabric: ~400 lines) instead of copying the runtime. |
| Tank amounts in the loader's unit (`McFluidTank`, Fabric droplets) | Other mods move fluid in that unit; keeping it avoids rounding at the boundary and keeps the saved NBT unchanged. |
| Code without Minecraft imports lives in `platforms/common`; code that names Minecraft but no Fabric API stays in `platforms/minecraft`; `Fabric*` names that held no Fabric code became `Mc*` | The split follows the imports: `common` is the code every loader needs and that cannot be Minecraft-specific; `minecraft` is the version-shared Minecraft code. NeoForge and Forge compile both already. |
| A loader layer may replace a shared file at the same path (`SyncedData` 1.21.9+, `RenderDataHolder` in `loom`) | The roots stack (`minecraft`, `common`, `loom`, the loader), so a loader-specific version of a shared seam needs no extra indirection. Shared changes for a range go to `platforms/minecraft/changes`, Fabric-API ones to `platforms/loom/changes`, loader ones to the loader's. |
| `DynamicUnbakedModel` stays in the `loom` layer | It only feeds Fabric's model loading plugin and creates the Fabric renderer's baked model; Quilt Loader uses the same Fabric API. |
| NeoForge and Forge jars carry the shared mixins without a mixin config | Wiring them belongs to those runtimes (not written yet); the metadata task only logs it instead of failing. |
| NeoForge and Forge carry their own detection sources | Each loader layer installs a version source and a platform source from its own API; the shared runtime calls `VersionDetector` and `PlatformDetector` from `modularity-api`. |
| NeoForge entry points: three `@Mod` classes (`common`, `Dist.CLIENT`, `Dist.DEDICATED_SERVER`), each taking `IEventBus`; init runs in `enqueueWork` | This is the documented NeoForge sides pattern: the loader constructs only the class whose dist matches, so no client type reaches a dedicated server. `enqueueWork` runs the init on the main thread after the parallel setup phase. |
| `ModularModRegistry` lives in `modularity-api`, not in `platform` | `UniversalJar` relocates `platform.*`; an adapter of another mod links only against the unrelocated API. |
| NeoForge and Forge seam stubs throw `UnsupportedOperationException` naming the method instead of registering nothing | NeoForge and Forge register content before common setup; a silent no-op would hide a lost registration. The follow-up (`modularity-m23h`) replays registrations. |
| Fabric entrypoints found in the assembled sources, not listed in `platforms.toml`: a class that directly implements `ModInitializer`, `ClientModInitializer`, `DedicatedServerModInitializer` or `PreLaunchEntrypoint`. Roles are `common`, `client`, `server`, `launch`; Fabric's keys are `main`, `client`, `server`, `preLaunch`, Quilt's are `main`, `client`, `server`, `pre_launch` | The class says that it is an entrypoint, so a rename or a version layer that adds or removes one needs no table edit. Each loader defines its keys, so the role names are internal and the mapping happens once, when the metadata is written. Direct implementation only: a class that inherits an interface from a base class is not found; the same limit as the mixin scan. |

## 7.9 Port log

What each version layer changes (oldest first). Version-specific calls sit behind the small classes of
`loom/compat` (and a few small client classes), so a layer replaces those, not the large files using them.

| Layer | Files | What changed in Minecraft / Fabric API |
|---|---|---|
| `changes/1.21.2+` | `RegistryCompat`, `GameCompat`, `compat/DamageEntity`, `ClientCompat`, `NamedValuesProperty`, `client/DynamicUnbakedModel`, `client/DynamicBakedModel`, `client/ModularEntityRenderer` | block/item properties need their registry key (`setId`), `EntityType.Builder#build(ResourceKey)`, `EntityType#create(level, EntitySpawnReason)` (we use `TRIGGERED`); `InteractionResult` is a sealed interface (`SUCCESS` on both sides); `PiglinAi.angerNearbyPiglins` takes the server level; `hurt` is final, entities implement `hurtServer`/`hurtClient`; `Input` record for held keys; `Property` has a value list and `getInternalIndex`; `UnbakedModel#resolveDependencies`, no `ItemOverrides`; entity renderers draw from an `EntityRenderState` |
| 1.21.3 | none | builds and runs from the 1.21.2+ layer unchanged |
| `changes/1.21.4+` | `compat/BoundsEntity`, `client/DynamicUnbakedModel`, `client/DynamicBakedModel` | `makeBoundingBox()` is final, entities override `makeBoundingBox(Vec3)`; block state models are `UnbakedBlockStateModel` baked with `ModelBaker#sprites`; Fabric Renderer API emits into a `QuadEmitter` with a cull test (no `RenderContext`); `BakedModel#isCustomRenderer` removed |
| `changes/1.21.5+` | `InterpolatedEntity`, `Nbt`, `EntityCompat`, `client/DynamicUnbakedModel`, `client/DynamicBakedModel` | `lerpTo` gone: server updates go through the entity's `InterpolationHandler` (ours forwards them to the existing lerp code); NBT getters return `Optional` (`...Or` keeps the defaults); `moveTo` is `snapTo`; `BakedModel` replaced by `BlockStateModel` (parts) and `BlockStateModel.UnbakedRoot`; Fabric emits through `FabricBlockStateModel#emitQuads`; sprites need a debug name |
| `changes/1.21.6+` | `SavedEntity`, `SavedBlockEntity`, `Inventories`, `SolidEntity`; smoke `Screenshots` | save data through `ValueInput`/`ValueOutput` (our compound stored with `CompoundTag.CODEC`, same format); `ContainerHelper` works on value IO (tag-backed `TagValueInput`/`TagValueOutput`); `canBeCollidedWith(Entity)`; `Screenshot.grab` takes a downscale factor |
| 1.21.7 | none | builds and runs from the 1.21.6+ layers unchanged |
| 1.21.8 | none | builds and runs from the 1.21.6+ layers unchanged |
| `changes/1.21.9+` | `KeyMappings`, `SyncedData`, `client/ModularEntityRenderer`, `client/SubmitPoseOutput` (new), `client/BufferPoseOutput.java.removed`, `mixin/client/ClientLevelMixin` (new), `mixin/client/ParticleEngineMixin.java.removed` | key mappings take a registered `KeyMapping.Category`; no vanilla compound entity data serializer (ours, registered with `FabricTrackedDataRegistry` as `modularity:compound_tag`); entity renderers `submit` to a `SubmitNodeCollector` (custom geometry) instead of writing to buffers; hit particles spawn in `ClientLevel#addBreakingBlockEffect` (was `ParticleEngine#crack`) |
| 1.21.10 | none | builds and runs from the 1.21.9+ layers unchanged |
| `changes/1.21.11+` | `Ids`, `SyncedData`, `client/SubmitPoseOutput`, `client/ObjModelReloadListener` | `ResourceLocation` is `Identifier`; render types come from `rendertype.RenderTypes`; vanilla quaternion serializer typed `Quaternionfc` (ours, `modularity:quaternion`, keeps `Quaternionf`); Fabric resource loader v1 (`ResourceLoader#registerReloader`) replaces the v0 identifiable reload listeners |
| `changes/26.1+` | `UsableEntity`, `LootCompat`, `CreativeTabs`, `Payloads`, `RenderDataHolder`, `BlockViews`, `Storages`, `KeyMappings`, `SyncedData`, `Messages`, `client/HudHook`, `client/FabricHudContext`, `client/DynamicUnbakedModel`, `client/DynamicBakedModel`, `client/ModularEntityRenderer`, `client/SubmitPoseOutput`, `client/ObjModelReloadListener`, `mixin/client/CameraMixin` | unobfuscated Minecraft, Java 25, Loom without remapping; Fabric API uses official names (`blockgetter`, `creativetab`, `keymapping`, `ContainerStorage`, `FabricEntityDataRegistry`, `serverboundPlay`/`clientboundPlay`, `registerReloadListener`); HUD only through `HudElementRegistry` with a `GuiGraphicsExtractor`; `Entity#interact` gets the hit point; loot `TOOL` is an `ItemInstance`; player messages split into system/overlay; block models in `block.dispatch` with baked `Material`s and material flags (ours: the delegates'); `entityCutout(id, outline)` is the no-cull type; camera zoom in `Camera#alignWithEntity` |
| 26.1.1 | none | builds and runs from the 26.1+ layer unchanged |
| 26.1.2 | none | builds and runs from the 26.1+ layer unchanged |
| 26.2 | `changes/26.2+`: HudHook, EntityIds + ServerLevelAccessor (EntityAccessor `.removed`); smoke Screenshots | hidden state and screen moved to `Minecraft.gui`; entity id counter moved to `ServerLevel`; main render target in `GameRenderer` |
| 26.3 | `changes/26.3+`: InterpolatedEntity, PoseOutput, ClientLevelMixin, PushReactions | InterpolationHandler is an interface made by `createInterpolationHandler`; `PoseStack.mulPose(Quaternionf)` replaced by `rotate`; breaking particles in `ClientLevel#addBreakingParticles`; `PushReaction` values renamed (`PUSH_PULL`, `POPPED`, `IMMOVEABLE`, `IGNORE_ENTITY`, `PUSH`), found by running refabslab on 26.3 (the smoke mod registers no blocks) |

## 7.10 Verification (2026-11-06)

`./gradlew buildAll collectJars --rerun-tasks` passes; `build/libs-all` holds `modularity-fabric-<version>` for all 16 versions plus `mvd-fabric-4.0.0` and `mpd-fabric-1.0.0`. Plugin tests: 27 passed. Layer checks (no identical copies, every `.removed` names a file below) pass on every platform. Fabric meta re-checked: 26.3 is the newest stable.

This record predates the merge of mvd and mpd into Modularity (docs/06). The detector mods and their jars
are gone; the same runs now expect the `[modularity] platform ...` line and the table stays as the
historical evidence. The re-run is an open check in `TODO-NOW.md`.

Files per layer (`.removed` markers in brackets): common 77; `1.21.2+` 8; `1.21.4+` 3; `1.21.5+` 5; `1.21.6+` 4; `1.21.9+` 5 (2); `1.21.11+` 4; `26.1+` 18; `26.2+` 3 (1); `26.3+` 3; no `versions/<v>` has sources. Smoke mod: common 4, `1.21.6+` 1, `26.2+` 1.

Smoke log lines (`build/smoke/{server,client}.txt`):

| Version | Mods loaded (not nested) | Version line (then) | Platform line (then) | Server end | Client |
|---|---|---|---|---|---|
| 21.1 | 3/3 | 1.21.1 | FABRIC 0.20.1 SERVER | ticked 40 | joined, screenshot, done |
| 21.2 | 3/3 | 1.21.2 | FABRIC 0.20.1 SERVER | ticked 40 | - |
| 21.3 | 3/3 | 1.21.3 | FABRIC 0.20.1 SERVER | ticked 40 | - |
| 21.4 | 3/3 | 1.21.4 | FABRIC 0.20.1 SERVER | ticked 40 | - |
| 21.5 | 3/3 | 1.21.5 | FABRIC 0.20.1 SERVER | ticked 40 | - |
| 21.6 | 3/3 | 1.21.6 | FABRIC 0.20.1 SERVER | ticked 40 | joined, screenshot, done |
| 21.7 | 3/3 | 1.21.7 | FABRIC 0.20.1 SERVER | ticked 40 | - |
| 21.8 | 3/3 | 1.21.8 | FABRIC 0.20.1 SERVER | ticked 40 | - |
| 21.9 | 3/3 | 1.21.9 | FABRIC 0.20.1 SERVER | ticked 40 | - |
| 21.10 | 3/3 | 1.21.10 | FABRIC 0.20.1 SERVER | ticked 40 | - |
| 21.11 | 3/3 | 1.21.11 | FABRIC 0.20.1 SERVER | ticked 40 | joined, screenshot, done |
| 26.1 | 3/3 | 26.1 | FABRIC 0.20.1 SERVER | ticked 40 | joined, screenshot, done |
| 26.1.1 | 3/3 | 26.1.1 | FABRIC 0.20.1 SERVER | ticked 40 | - |
| 26.1.2 | 3/3 | 26.1.2 | FABRIC 0.20.1 SERVER | ticked 40 | - |
| 26.2 | 3/3 | 26.2 | FABRIC 0.20.1 SERVER | ticked 40 | - |
| 26.3 | 3/3 | 26.3 | FABRIC 0.20.1 SERVER | ticked 40 | joined, screenshot, done |
