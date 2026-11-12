# 7. Multi-version build (Fabric)

Modularity ships one Fabric platform jar per Minecraft version, from 1.21.1 to the latest stable
release. The code is written once and layered: Fabric code common to every version, then only the
changes per version range, then the few details of a single version. Our own Gradle plugin
(`lv.cebbys.modularity`, in `modularity-gradle/`) assembles every platform from these layers.

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
modularity-gradle/                               included build: the lv.cebbys.modularity plugins
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
modularity-smoke/minecraft | loom | fabric | quilt | neoforge | forge
                                           smoke-test mods (same layering, see 7.6); the guest mod in
                                           modularity-smoke/src/main (the :modularity-smoke module)
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
| neoforge | `minecraft`, `common`, `anvil`, `neoforge` | yes |
| forge | `minecraft`, `common`, `anvil`, `forge` | yes |

`minecraft` holds Minecraft code; `common` holds code that every loader shares and that does not belong
to Minecraft (entrypoint roles, lifecycle, seam registry, guest descriptor); `loom` holds the Fabric API
code that runs on both Fabric and Quilt Loader. `anvil` holds the code that NeoForge and Forge share:
the stage order, the seam holder and the detection sources. It names no loader type, because the two
Forge-family APIs live in different packages; what the loaders do differently stays in `neoforge` and
`forge`.

Code that needs a loader API goes through the seams in `platform.loader`, installed by the loader's entrypoint
through `Loaders.install` (client: `installClient`):

| Seam | Covers | Loom implementation | NeoForge implementation | Forge implementation |
|---|---|---|---|---|
| `LoaderInfo` | side, loaded mods and versions, development mode, mods with a `modularity` entrypoint | `ModularityLoomLoaderInfo` | `NeoForgeLoaderInfo` | `ForgeLoaderInfo` |
| `RenderDataAccess` | block entity render data for models, also off the game thread | `ModularityLoomRenderData` | `NeoForgeRenderData` | `ForgeRenderData` |
| `StorageBackend` | fluid unit, exposing entity tanks and inventories, filling a tank from a held item and draining it into one | `ModularityLoomStorageBackend` | `NeoForgeStorageBackend` | `ForgeStorageBackend` |
| `LoaderHooks` / `ClientHooks` | registration, block entity types, creative tabs, payloads; key mappings, entity renderers, dynamic block models, HUD, reload, client tick | `ModularityLoomLoaderHooks` / `ModularityLoomClientHooks` | `NeoForgeLoaderHooks` / `NeoForgeClientHooks` | `ForgeLoaderHooks` / `ForgeClientHooks` |

Every seam of both Forge-family loaders does real work. Dynamic block models have a different shape on each family.
Both Forge-family loaders give a baked model the block position through `getModelData`, so
`NeoForgeDynamicBakedModel` and `ForgeDynamicBakedModel` compute the emitted states there and return the quads of
those states from `getQuads`. `ModelEvent.ModifyBakingResult` replaces the baked model of every state of the block.
The Fabric path instead emits through the Fabric Renderer API (`DynamicBakedModel`).

NeoForge constructs the mod entry points as three `@Mod` classes — common, `Dist.CLIENT` and
`Dist.DEDICATED_SERVER` — so a dedicated server never loads client types. Each class registers its setup
event and runs the init in `enqueueWork`. Forge has one `@Mod` class, because its annotation has no side
filter: the constructor asks `FMLEnvironment.dist` and registers the client entrypoint or the dedicated
server entrypoint, and only the branch that runs loads that class.

The two loaders reach registration, capabilities and networking through different APIs, so each loader root
keeps its own wiring:

| Area | NeoForge | Forge |
|---|---|---|
| Registration | `RegisterEvent`, replayed per registry | `RegisterEvent`, replayed per registry |
| Entity storages | `RegisterCapabilitiesEvent.registerEntity` | `AttachCapabilitiesEvent<Entity>` with a `LazyOptional` provider per entity |
| Payloads | `RegisterPayloadHandlersEvent`, `registrar.playBidirectional` | `ChannelBuilder…payloadChannel()`, one built `Channel` |
| Client events | the mod bus and `NeoForge.EVENT_BUS` | the mod bus and `MinecraftForge.EVENT_BUS` |
| HUD layers | `RenderGuiEvent.Post` | `AddGuiOverlayLayersEvent` and `ForgeLayeredDraw` |

A higher root may replace a shared file at the same path (`loom` does for `SyncedData` from 1.21.9 and for
`RenderDataHolder`). The Quilt entrypoints implement the same Fabric interfaces for `main`, `client` and
`server`, and native Quilt `pre_launch` for the launch role.

Fabric-family audit (stage 1 of the port programme). Each functionality area has one home in `loom`; the
loader root holds only what names the loader itself:

| Functionality | `loom` classes | Loader-root part |
|---|---|---|
| Lifecycle and entrypoints | `ModularityLoomCommon`, `ModularityLoomClient`, `ModularityLoomServer`, `ModularityLoomLaunch`, `ModularityLoomSeams` | the four one-line entrypoints of `fabric` and `quilt` |
| Detection | `ModularityLoomClasses`, `ModularityLoomPlatformSource`, `ModularityLoomVersionSource` | none |
| Content registration | `ModularityLoomLoaderHooks`, `compat.BlockViews`, `compat.CreativeTabs` | none |
| Networking | `compat.Payloads`, the payload methods of both hooks | none |
| Storage | `ModularityLoomStorageBackend`, `ModularityLoomTankStorage`, `compat.Storages` | none |
| Client | `ModularityLoomClientHooks`, `HudHook`, `DynamicBakedModel`, `DynamicUnbakedModel`, `ObjModelReloadListener`, `compat.KeyMappings` | none |
| Mixins | the mixin sources of `minecraft/mixin` and the config the build generates | the metadata of the loader |
| Guest mods | `guest.GuestCandidates` | `fabric/guest/FabricGuestJson` and `LoaderInjection` (Fabric Loader internals) |
| Verification | the smoke setup and task of `modularity-gradle` | the platform's `smokeServer` and `smokeClient` |

Forge-family audit (stages 3 to 5 of the port programme). Only what both loaders can share lives in `anvil`;
every other area is in the loader roots:

| Functionality | `anvil` classes | Loader-root part |
|---|---|---|
| Lifecycle and entrypoints | `ModularityAnvilLifecycle` (the stage order), `ModularityAnvilSeams` (the seam holder) | the `@Mod` classes of `neoforge` and `forge`, each with its own events |
| Detection | `ModularityAnvilLoaderApi`, `ModularityAnvilPlatformSource`, `ModularityAnvilVersionSource` | none |
| Content registration | none | `NeoForgeLoaderHooks`, `ForgeLoaderHooks` |
| Networking | none | `NeoForgePayloads`, `ForgePayloads`, the payload methods of both hook pairs |
| Storage | none | `NeoForgeStorageBackend`, `ForgeStorageBackend` and their tank handlers |
| Client | none | `NeoForgeClientHooks`, `ForgeClientHooks`, `NeoForgeRenderData`, `ForgeRenderData` |
| Mixins | none | the `[[mixins]]` block of the NeoForge metadata and the `MixinConfigs` manifest attribute of Forge (generated metadata and jar), each with its own generated config (7.8) |
| Guest mods | none | none on either loader |
| Verification | none | the platform's `smokeServer` and `smokeClient` |

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

Two plugins in `modularity-gradle/`:

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
metadata and its own mixin config. Neither loader needs a refmap (both run in the official namespace). NeoForge
applies its config in a development run; Forge names its config in the jar manifest and the config is
generated the same way (owner decision of 2026-11-12, 7.8). The Forge smoke must still show it
applying. Beans `modularity-9rzu` (NeoForge), `modularity-6rm0` (Forge) and `modularity-xbj0` (the
shipped jar).

## 7.5 Runtime detection

Modularity detects the Minecraft version and the loader itself (docs/06). `api.detect` holds the
detector, the source interface and the built-in sources; each loader layer holds its own source and
installs it in the entrypoint. The detector runs the installed sources first and the built-in ones
after them, and caches the first result. Each install point logs one line,
`(<side>) platform <loader>:<loaderVersion>, minecraft:<version>`.

Detection uses explicit `install(...)` and not `ServiceLoader`, so it needs no service file. The jar has one
service file, and it is not for detection: `META-INF/services/net.minecraftforge.forgespi.locating.IDependencyLocator`
names Forge's locator (7.8). `UniversalJar` rewrites it for the relocated tree, like a metadata file. The
file is generated for the jar alone (`forgeLocatorService`, `build/generated/forge-locator`), never for the
source set: on the development class path FML reads that entry as a transformer artifact, and the main
output then stops being a mod (7.8, run of 2026-11-12).

## 7.6 Smoke tests

The smoke mod lives in `modularity-smoke/` and is layered like the platform. Its guest mod is the
`:modularity-smoke` jar; the platform smoke runs take that file as `modularity.guestMods`. Each
platform project gets a `smoke` source set and two runs:

* `smokeServer`: dedicated server, flat world, stops itself after a few ticks; the task then checks
  the log: Modularity loaded as a top-level mod, the detection line naming the exact version and
  the loader (`FABRIC`, `QUILT`, `NEOFORGE`, `FORGE`), and the entrypoint stage lines of the loader.
* `smokeClient`: client joins the world written by the server run, takes a screenshot, quits; same
  log checks.

The stage lines are `Entrypoint launch active (<loader>)` and `Entrypoint common active (<loader>)` on
Fabric and Quilt, with `Entrypoint mod-constructor active (<loader>)` before them on NeoForge and Forge,
because the Forge family runs the detection and the launch stage in the mod constructor and the common
stage on the first `RegisterEvent` (7.8). After the platform line the
check expects `Entrypoint server active (<loader>)` or `Entrypoint client active (<loader>)`. The check
does not enforce the order; the log does.

The smoke mod is also a Modularity mod (`SmokeFeatures`). Its common stage declares a block, an item and a
payload channel and logs `Smoke features: declared block=… item=… channel=…`; the common stage of the side
resolves the block and logs `Smoke features: resolved <side> block=… present=true`. The check expects both
lines on every loader: Fabric and Quilt discover the mod through the `"modularity"` entrypoint, NeoForge and
Forge register it in the smoke mod constructor (7.8, bean `modularity-4uol`).

The same channel proves both payload directions in one round trip. The common stage registers
`channel.onServer`: the handler logs `Smoke network: server received 42` and sends the value back with
`channel.sendTo(player, value)`. The client stage registers `client.onMessage`, which logs
`Smoke network: client received 42`, and a `client.onTick` handler that sends 42 with
`channel.sendToServer` on the first tick that has a player. The client check expects both network lines on
every loader. Only the client run has an integrated server, so the dedicated-server check expects neither.
The 42 is `SmokeFeatures.MESSAGE`; a codec that changes the value fails the check.

The side stages also prove the loader's fluid unit. `SmokeFeatures.storage` builds an `McFluidTank` with
`Loaders.storage().unitsPerMb()`, fills 1000 mB and drains 400 mB, then logs
`Smoke storage: unitsPerMb=… inserted=… units=… drained=… amount=…`. The check reads the numbers and not
only the line: the tank must keep exactly `1000 × unitsPerMb` units (Fabric droplets, 81 per millibucket;
1 on NeoForge and Forge), and the portable view must report 600 mB after the drain. A tank that keeps no
unit or loses the conversion fails.

Two further `StorageBackend` methods need a world, so the client run proves them and the dedicated-server
check expects neither. `fillFromHeldItem` runs once on the server thread: the server player holds a water
bucket in the main hand, the loader moves the fluid into a fresh tank, the emptied item goes back into the
hand, and the line `Smoke storage: held item moved 1000 mB` reports the millibuckets that moved.
`exposeEntityStorages` runs once after the entity renderer saw the spawned entity: the smoke entity carries
a 4000 mB tank, and the client asks the lookup of the running loader — Fabric `EntityApiLookup`, NeoForge
and Forge the entity fluid capability — for 500 mB. The line `Smoke storage: exposed 500 mB` therefore
proves the registration of the loader and not a Modularity table. The check requires both numbers, so a
seam that moves nothing and a lookup that finds no tank both fail.

The smoke runs the whole cycle with three right-clicks on the entity, ten ticks apart, each one a normal
player action through `MultiPlayerGameMode.interact`:

| Click | Item in hand | Lines |
|---|---|---|
| 1 | water bucket | `Smoke storage: held item moved 1000 mB`, `Smoke storage: entity tank holds 1000 mB` |
| 2 | empty bucket | `Smoke storage: bucket took 1000 mB`, the tank drops to 0 and the hand holds the water again |
| 3 | empty bucket | `Smoke storage: bucket took 0 mB` — the tank is empty, so the pickup moves nothing |

`SmokeStorageProof` is the entity behaviour behind it: `interact` fills from a full bucket through
`StorageBackend.fillFromHeldItem`, and takes the fluid out through `StorageBackend.drainToHeldItem` when the
hand holds an empty bucket. The behaviour passes on the client, because the client also runs the interaction
as a prediction and only the logical server owns the tank. The check requires all six storage lines, so a
bucket that does not empty, a tank that keeps the fluid, a drain that reports nothing, and a second pickup
that still moves fluid each fail the smoke.

The client check also reads the screenshot. A `Screenshot:` line only says a file was written, so
`SmokeCheck` reads the PNG and rejects an empty, unreadable or too small image (under 320 × 240) and one
that holds 16 colours or fewer, which is a flat screen. The client smoke writes it to
`<run dir>/screenshots/smoke.png` and `SmokeSetup` passes that path to the check. The check proves that
the client drew a frame; it does not prove what the frame holds, so a dynamic block model still needs a
scenario of its own.

The client stage of `SmokeFeatures` also calls the rest of the client seam and logs one line for each
call. `client.blockModel` registers a model decided per position and logs `Smoke model: registered for
<block>`; `client.hud` registers a layer that draws one square on the first frame and logs
`Smoke client: hud drawn <width>x<height>`; `client.keyBinding` registers a key with default `J`, and
the first tick with a player logs `Smoke keys: binding=<name> down=<true|false>`. `client.hitParticles`
registers the handler that chooses the particle texture of a block being mined.

On the Forge family the client stage runs at the end of the common stage, inside the first
`RegisterEvent` (7.8). NeoForge and Forge post their client registration events while the game object is
constructed, before any setup event, so a client stage that waited for `FMLClientSetupEvent` added its
listeners too late. The block and entity registries are not readable at that point, so a client
registration keeps the id of its block or entity type, and the loader resolves the native object when its
own event fires. The smoke therefore resolves its block on the first client tick, not in the client
stage.

A scenario then puts the block in the loaded world, so the paths that need a real block run.
`SmokeScenario` waits for the first tick with a player and places the block at head height, three blocks
in front (`player.blockPosition().offset(0, 1, 3)`), so a level view reaches it and it stands clear of the
floor. The placement runs on the server thread, and the scenario logs `Smoke scenario: placed <block> at
<x, y, z>`. The game takes the mouse when a run starts, so the scenario releases it on the first tick and
re-aims the player on every mining tick: a moving mouse cannot turn the player away from the block. From
tick 30 to tick 110 it holds the mine button through `Minecraft.continueAttack(true)`, which is the path
a player takes. The dynamic model answers the first render pass and logs `Smoke model: quads at <pos>`,
and the hit-particle handler answers the first hit particle and logs `Smoke particles: hit <block> at
<pos>`. Vanilla spawns those particles only from `Minecraft.continueAttack`, not from
`MultiPlayerGameMode`, so the smoke must take the same path as the player. The client check expects all
three lines, so a model that is registered but never renders, or a hit-particle handler that never runs,
fails.

The block also spawns the registered entity when a player breaks it, so the entity chain runs in the
game: the common stage registers the entity type and logs `Smoke entity: registered for <id>`, the break
spawns it on the server and logs `Smoke entity: spawned <id> at <pos>`, and the entity renderer of the
loader draws it and logs `Smoke entity: rendered <id>`. The renderer also draws the OBJ model of the
smoke mod, a unit cube, with `RenderOutput.model` and a vanilla texture, so the run shows a real object on
the screen; the first draw logs `Smoke obj: drawn <id> faces=<n>`. The client check expects all four lines.

The OBJ path is one more proof of the client seam, and it is shared code: `ClientContext.objModel` returns
`McModelRef` (7.3), `ObjParser` reads the Wavefront file, `RenderOutput.model` sends its faces to the
loader's frame, and `ModularityClient.init` clears the cache through `ClientHooks.onResourceReload` on
every loader. The smoke registers `modularity_smoke:models/obj/smoke_cube.obj`, parses it on the first
client tick and logs `Smoke obj: parsed <id> faces=6 groups=1`. The client check requires a non-empty parse
and a draw, so a resource that does not reach the mod and a model that never draws both fail.

`SmokeScenario` changes a block and mines it, and those calls move between Minecraft versions while the
smoke sources compile for every version of the table. The class finds `setBlockAndUpdate`,
`continueAttack`, `getSingleplayerServer`, `releaseMouse`, `setYRot` and `setXRot` by name and logs
`Smoke scenario: this Minecraft version has no …` when a version does not provide one. A red smoke then
names the missing method instead of failing to compile.

The shared smoke sources live in `modularity-smoke/minecraft`, `modularity-smoke/loom` and the loader
roots; a root without a `modularity-smoke/<root>` folder is skipped. The smoke guest mod loads on Fabric only (docs/08), so
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
| `loom` root for Fabric and Quilt; `anvil` root for NeoForge and Forge | Fabric API code that both Fabric and Quilt Loader run needs one home. `anvil` holds the Forge-family stage order (`ModularityAnvilLifecycle`), the seam holder (`ModularityAnvilSeams`) and the detection sources (`ModularityAnvilLoaderApi`). It names no loader type, because NeoForge and Forge publish their APIs in different packages; a file that needs one of them stays in `neoforge` or `forge`. |
| Quilt has its own class set, `quilt.mod.json`, entrypoints and mixin config in the universal jar | Full loader separation: Quilt Loader reads native metadata, not Fabric's; the universal jar carries `fabric.mod.json` and `quilt.mod.json` side by side. Quilt Loader 0.30.2 dispatches `main`/`client`/`server` to `ModInitializer`/`ClientModInitializer`/`DedicatedServerModInitializer` and `pre_launch` to `PreLaunchEntrypoint` (checked in the loader's sources), so the Quilt entrypoints reuse the same interfaces for the first three roles. |
| Quilt uses Fabric API as its library; QSL is not used | QSL for 1.21.1 is alpha-only and discontinued, and it lacks transfer/fluid, item and entity lookups, client events and model loading. Quilt Loader provides the Fabric Loader API, so `loom` compiles once. |
| The universal jar carries one full class set per loader, Quilt included | Real separation costs the duplicated shared `common`/`loom`/`minecraft` classes. Sharing the Fabric tree instead would leak Quilt overrides into Fabric. |
| A native `quilt.mod.json` is generated only when the platform is remapped (Fabric/Quilt on an obfuscated version) | The QMJ names `net.fabricmc:intermediary`; a 26.x Loom platform uses Mojang names, so the metadata task fails with a clear message until the Mojang-mapping path is designed. |
| `quilt_loader.jars` is added to the built jar, not to the generated resources | Quilt Loader in development reads the metadata from `build/resources/main` and opens every nested jar it names; Loom creates the nested jars only in the jar task. Loom does the same for `fabric.mod.json`; `QuiltJarMetadata` does it for the QMJ after the jar is built. |
| Quilt through Fabric Loom with Quilt Loader as the loader dependency, no Quilt Loom | One Loom for the whole build. Loom reads loader libraries only from `fabric-installer.json`; the plugin adds the `common` and `development` libraries of Quilt Loader's own `quilt_installer.json`, copies Loom's `fabric.*` development properties in `launch.cfg` to Quilt's `loader.*` names, and sets Quilt's `KnotClient`/`KnotServer` main classes. |
| Quilt platforms compile against Fabric Loader (compile only) and exclude it at runtime | `loom` uses the Fabric Loader API (`ModularityLoomLoaderInfo`, `GuestCandidates`), which Quilt Loader provides; at runtime only Quilt Loader may be present (both ship `net.fabricmc.loader.impl`). |
| `fabricloader >=0.16.0` in the Fabric jars (`fabric-loader-min`), built and run with 0.20.1 | Quilt Loader 0.30.2 reports Fabric Loader 0.19.5, so `>=0.20.1` refused the Fabric jar on Quilt. The loader API Modularity uses is old and stable. Only guest mods depend on 0.20.1 internals (docs/08). |
| Quilt, NeoForge and Forge only for 1.21.1 for now | Scope set by the project owner. Quilt was also tried on every other version: 1.21.2 to 1.21.11 passed on Quilt Loader 0.30.2; 26.x needs Quilt Loader 0.31.0-beta.6 (0.30.2 loads the mods there but never calls their entrypoints) and then passed too. |
| No guest mods on Quilt yet | The guest loader uses Fabric Loader internals that Quilt Loader does not have; on Quilt it logs an error and loads no guests. Quilt smoke tests run without the guest. |
| NeoForge with ModDevGradle 2.0.148, Forge with ForgeGradle 7.0.40, both on the shared plugin classpath next to Loom | The plugins and versions of NeoForge's and Forge's own 1.21.1 MDKs, both on Gradle 9. ModDevGradle's legacy Forge plugin cannot build Forge 1.21.1 and ForgeGradle 6 needs Gradle 8. All three plugins in one build were tried first in a throwaway build. |
| NeoForge/Forge metadata (`neoforge.mods.toml`, `mods.toml`) generated from the same `[mod]` values; Minecraft exact, the loader from its line on (`[21.1,)`, `[52,)`), `>=X` ranges as `[X,)` | One description of the mod for every loader. |
| `[mod] mods` lists the Fabric modules; NeoForge and Forge use their `-neoforge` / `-forge` sibling | The separate mods are the same mods per loader. |
| Smoke mods layered like the platforms (`modularity-smoke/minecraft` + the loader's root) | The screenshot code is plain Minecraft code and its version changes apply to every loader. |
| NeoForge smoke source set: own configurations extending `main`'s, plus `addModdingDependenciesTo` | ModDevGradle adds its dev launcher to the source set's configurations; a plain file class path misses it. |
| Forge runs named `server` / `client`, smoke tasks `runSmokeServer` / `runSmokeClient` | ForgeGradle 7 fills a run from Forge's run configuration of the same name and registers one task per source set (`run<SourceSet><Name>`) with that source set's class path; a run's `classpath` option replaces the launcher's own class path. |
| Forge: resources compiled into the classes folder (`main`, `smoke`) | Forge's development discovery takes each class path folder as a mod file; `mods.toml` must sit next to the classes. |
| Forge jar-in-jar written by the plugin (`META-INF/jarjar` + `metadata.json`, same format as ModDevGradle) | ForgeGradle 7 has no jarJar; Forge 52 reads that format (`JarInJarDependencyLocator`). Loading the nested API from a production jar is still to be checked in a real Forge install. |
| Forge family: the common stage runs on the first `RegisterEvent`, not in Modularity's mod constructor and not on `NewRegistryEvent` | NeoForge and Forge close their registries before common setup, so the common stage must run before the freeze that precedes it. The mod constructor is too early, because `ModularityCommon.init` freezes `ModularModRegistry` there, before any other mod is constructed, and a dependent mod could not register (bean `modularity-4uol`). `NewRegistryEvent` is also too early, and a run of 2026-11-12 proved it: NeoForge `CommonModLoader` posts `RegistryManager.postNewRegistryEvent()` **before** `GameData.unfreezeData()` and only then `GameData.postRegisterEvents()`, so every vanilla registry is still frozen in that event, and `Block.<init>` throws `IllegalStateException: Registry is already frozen` at `MappedRegistry.validateWrite` (the same failure on Forge). The first `RegisterEvent` is the writable window, and it is posted after every mod constructor, so a dependent mod can still register itself there. The entrypoints guard with a `boolean`, run `launchMods` and `common` once, then call `replay(event)` on the hook of that loader, because its own listener has already run for that registry. The detection and the launch stage stay in the constructor. |
| `pack.mcmeta` (pack format 34) in the Forge layer, smoke mod and utility mods | Forge 1.21.1 warns for mods without it; the value is the one of Forge's own 1.21.1 MDK. |
| NeoForge/Forge: the client stage runs at the end of the common stage, inside the first `RegisterEvent`, and a client registration keeps an id that the loader resolves in its own event | NeoForge and Forge post `EntityRenderersEvent.RegisterRenderers`, `RegisterKeyMappingsEvent`, `RegisterClientReloadListenersEvent` and Forge's `AddGuiOverlayLayersEvent` from `ClientHooks.initClientHooks` while `Minecraft` is constructed. `CommonModLoader.load` posts `FMLCommonSetupEvent` and `FMLClientSetupEvent` later, during the first resource reload, so a client stage on `FMLClientSetupEvent` adds its listeners after those events have fired: the smoke run of 2026-11-12 crashed with `NullPointerException … "entityrenderer" is null` at `EntityRenderDispatcher.shouldRender`. No Forge-family event exists between the end of the registry pass and the client hooks, so the client stage must run inside the registry pass. The block and entity registries are not readable there, so `ClientHooks.blockModels` and `ClientHooks.entityRenderer` take an id and the loader resolves the block or the entity type when its own event fires; `ClientRegistry` keys its tables by id for the same reason. A mod's client stage therefore sees the registrations of the pass up to that point, as the common stage already does, and the smoke resolves its block on the first client tick. |
| NeoForge/Forge utility mods: the bundled core is not also a runtime dependency | Both load mods as Java modules; the core's package may come from one module only (Forge refused the split package). |
| Forge smoke "nested" check by jar path (`META-INF/jarjar/`) | Forge's mod file API names no parent file (NeoForge's does). |
| `modularity-gradle` compiled for Java 25 | Loom 1.19.3 is published as Java 25 bytecode, so plugins depending on it must be too. |
| Mixin config `compatibilityLevel` JAVA_21 for every platform | The mixins use no newer language features; one value every shipped Mixin knows. |
| Mixins listed from `@Mixin` classes in the assembled sources, `client.*` under `client` | A version layer can add or remove a mixin without editing a config. |
| Mixin names sorted as strings | Windows sorts paths case-insensitively; the generated config must be identical on every OS. |
| Forge: the `MixinConfigs` attribute also sits in the generated metadata, and the jar task excludes that copy | A development run scans the classes folder as a mod file and reads the manifest of that folder, so the attribute of the jar alone never reaches Mixin: the Forge client smoke of 2026-11-12 held no `Mixing` line. `GenerateModMetadataTask` writes `META-INF/MANIFEST.MF` with `MixinConfigs` for the Forge platform, and `ForgePlatformSetup` excludes that file from the jar copy, so the jar keeps the attribute of its own manifest. The next Forge client run logs seven `Mixing … from modularity.forge.mixins.json` lines (7.10). |
| Forge: the HUD layer is appended to the overlay stack, not anchored above the hotbar | `ForgeLayeredDraw.addAbove(id, ForgeLayeredDraw.HOTBAR, …)` found no `minecraft:hotbar` in `minecraft:vanilla_root` on Forge 52.1.21 and logged `Expected layer minecraft:hotbar was not found in stack minecraft:vanilla_root, no layer modifications have been made`, so the client smoke held no `Smoke client: hud drawn` line. `ForgeLayeredDraw.add(id, layer)` appends to the root stack and draws after the vanilla HUD; the next client run logs `Smoke client: hud drawn 427x240` (7.10). |
| OBJ models are shared code; the anvil root has no copy | `ClientContext.objModel` returns `McModelRef` and `ObjParser`/`ObjModel` live in the API (7.3), and the cache is cleared through `ClientHooks.onResourceReload`, which the anvil implements with `RegisterClientReloadListenersEvent` + `RunnableReloadListener`. Only the Fabric-flavoured wrapper `ObjModelReloadListener` is loom-only, because `loom` needs a Fabric reload listener object; the anvil needs none. The smoke of 2026-11-12 therefore proves the path on all four loaders by parsing `modularity_smoke:models/obj/smoke_cube.obj` and drawing its six faces with `RenderOutput.model`, and the screenshot shows the textured cube (7.6, 7.10). |
| A mod entrypoint class must not name client vanilla packages | Quilt Loader reads the `modularity` entrypoint of a mod in `pre_launch` and refuses a class that references a client package: the run of 2026-11-12 failed with `Cannot load package net.minecraft.client.server because Mismatched @Environment`. Fabric accepts it. The smoke therefore keeps `SmokeFeatures` free of client vanilla types and puts client-only work in `SmokeScenario`, which the client path loads (7.6). A mod built on Modularity follows the same split. |
| A `StorageBackend` tank accepts a null transaction on Fabric | The Transfer API documents a null `TransactionContext` as "no transaction", but `SnapshotParticipant.updateSnapshots(null)` throws (`TransactionContext.nestingDepth()` on null). `ModularityLoomTankStorage` applies the change and notifies when the context is null, and the smoke opens a real outer transaction, which is what another mod does. Found by the run of 2026-11-12, which no earlier test reached. |
| Forge needs `addGenericListener` for `AttachCapabilitiesEvent` | `MinecraftForge.EVENT_BUS.addListener` refuses a generic event listener: `Cannot register a generic event listener with addListener, use addGenericListener`. `ForgeStorageBackend.exposeEntityStorages` therefore calls `addGenericListener(Entity.class, …)`. Found by the run of 2026-11-12. |
| Forge fills from a held bucket through `FluidBucketWrapper` | In Forge 52.1.21 the capability path moves nothing from a vanilla bucket: `FluidUtil.tryEmptyContainerAndStow` reported no success and the tank stayed empty (`held item moved 0 mB`, run of 2026-11-12), and the hand kept the full bucket. The seam reads the capability, falls back to `FluidBucketWrapper` for a `BucketItem`, drains the item into the tank and puts the emptied container back in the hand; the screenshot of 2026-11-12 shows the empty bucket in the hotbar. NeoForge needs no fallback. |
| `StorageBackend.drainToHeldItem` is the fourth seam method | The seam covered the unit, the exposure and the fill, so a mod had no way to take fluid out of a tank into a bucket through the API. The fourth method moves fluid from the tank into the item in the main hand and returns the millibuckets; Fabric moves the droplets in one transaction and swaps the bucket in a second one, NeoForge and Forge read the item capability, fall back to `FluidBucketWrapper` for a plain bucket and put the filled container back in the hand. The smoke proves it with the three-click cycle (7.6, 7.10). |
| A tank handler reads its fluid before it drains | `ForgeTankHandler` and `NeoForgeTankHandler` built the returned stack from `tank.nativeFluid()` after emptying the tank, and an empty tank holds `Fluids.EMPTY`, so the caller got an empty stack and reported `0 mB` although the fluid left. The run of 2026-11-12 showed the tank falling from 1000 to 0 with `bucket took 0 mB`. Both handlers now capture the fluid first. |
| The Fabric item swap of a drain is a second transaction | On Quilt, `tank.nativeFluid().getBucket()` gave a blank variant and `ContainerItemContext.exchange` threw `Transfer variant may not be blank`; because the throw was inside the move transaction, the whole drain rolled back. The move now commits on its own, the swap is guarded, and water maps to `Items.WATER_BUCKET` without asking the fluid instance. |
| Loader identity travels as the `Loader` enum, never as a string | `Loader` already existed in `api.core` for `Platform.loader()`, but every entrypoint passed `"fabric"`, `"quilt"`, `"neoforge"` or `"forge"` by hand through `EntryPoints`, `ModularityCommon`, `ModularityLaunch` and the `loom`/`anvil` stages. A string accepts a typo that only a run finds. `Loader.id()` gives the lower-case name for the log lines, so the logs stay the same. `LaunchContext.loader()` now returns the enum too. |
| 21.1 move verified by jar diff | Every class in the 21.1 jar is byte-identical before and after the move; only `fabric.mod.json` (exact `minecraft`, `fabricloader >=0.20.1`) and the order of two client mixins changed. |
| `assembleLayers` inputs are only `*/src/<set>/**` of the layers | `versions/<v>` is also the project dir; its `build/` and `run/` must not be inputs. |
| Smoke tests built before the ports (goal step 7 moved before step 5) | Every port commit is verified with the same `smoke` task for that version and all earlier ones. |
| Smoke mod as source set `smoke` of each platform, own Loom runs in `build/smoke/run` | Same classpath and mappings as the platform; no second build, no maven round trip; each source set is its own mod (Loom `mods {}`). |
| Fresh flat world per server run; the client opens that world with `--quickPlaySingleplayer` | The client test needs no world creation UI; the world matches the version. |
| Log checks in `SmokeCheck` (unit tested): Modularity loaded and not nested, `(<side>) platform <loader>:<loaderVersion>, minecraft:<exact>`, end marker, no crash or failed mixin | The goal's verification, done mechanically; matched lines go to `build/smoke/{server,client}.txt`. |
| `tools/smoke-fabric-1.21.1` kept | It is the refabslab integration test (double slabs, KleeSlabs), separate from the platform smoke tests. |
| One decompilation per version, by the Fabric platform (the parent's `genSources` points there); it is not part of `build`, and the other platforms' tasks `mustRunAfter` it | Loom keeps one mapped Minecraft jar and one Maven directory per version in the root `loom-cache` and declares the directory an output of `genSourcesWithVineflower`; Gradle deletes declared outputs before running a task. Decompiling inside a parallel build therefore deletes files the other platforms of the version read, so they wait for it, and `build` leaves decompilation to an explicit `genSources` run. Sources after compilation is impossible (Loom's `genSources` depends on its own `remapJar`). The mappings are the same for every loader of a version, so one decompilation serves them all. No platform source depends on the decompiled output, so `genSources` is run on its own. |
| Parchment layered over the official mappings, one entry per Minecraft version in `[parchment]` | Parameter names and javadoc in the Minecraft classes the platforms compile against, without changing names, remapping or runtime: the universal jar is byte-identical. It is published per Minecraft version, not per loader, so one entry covers Fabric, Quilt, NeoForge and Forge; versions without a release (1.21.2, 26.x) build with the official mappings alone. Shape: Loom `layered` for Fabric/Quilt, ModDevGradle `neoForge.parchment` for NeoForge, ForgeGradle `minecraft.mappings("parchment", …)` for Forge. ForgeGradle's run launcher resolves the run class path in an attribute-less detached configuration that no project configuration hook can reach, so on Forge the mappings channel and version are additionally set as attributes on the Minecraft dependency; that is what makes the two published variants of `net.minecraftforge:forge` resolvable there. |
| CI: one parallel job per enabled platform, then a final job; smoke jobs get JDK 8, 21 and 25 and run under xvfb for client runs | Per-platform results stay visible and the jobs run at the same time. The matrix comes from `gradle/platforms.toml`, so the table stays the only source. A per-platform job cannot make the universal jar, which merges every loader of a version, so the final job runs `collectJars` with every platform. Loom 1.19 needs Gradle on 25; 1.21.x toolchains need 21; ForgeGradle's slime launcher needs 8. setup-java writes all three to `~/.m2/toolchains.xml`, where Gradle finds them. |
| Detection code compiled into `modularity-api` at release 17 | It uses no Minecraft and no loader class; the loader sources live in the loader layers. |
| Version calls behind small classes in `loom/compat` (seams), added to the `common` layer in their own commits before the port that needs them | A layer then replaces a 20-line seam instead of a large file, so "only changes per version" holds and big classes stay in the `common` layer once. |
| NeoForge runs Modularity's launch stage in the mod constructor and the common stage on the first `RegisterEvent`, and queues every registration for `RegisterEvent` | NeoForge freezes the vanilla registries at the end of mod construction and unfreezes them for the `RegisterEvent` pass only (`CommonModLoader`: `postNewRegistryEvent`, then `unfreezeData`, then `postRegisterEvents`), so an eagerly constructed value fails earlier. `NeoForgeLoaderHooks` stores each registration with its registry key and replays it when `RegisterEvent` fires for that key. The event is later than the constructor, so a dependent mod can register itself first (bean `modularity-4uol`). The stage still runs before the world exists, so it must stay free of world and player state. |
| NeoForge entity storages through `RegisterCapabilitiesEvent` (`Capabilities.FluidHandler.ENTITY`, `Capabilities.ItemHandler.ENTITY`), fluids in millibuckets | Capabilities are NeoForge's own lookup; a listener added during the common stage is replayed when the event fires. `unitsPerMb()` is 1, so tank units are millibuckets and no conversion is needed. Filling from the held item uses `FluidUtil.tryEmptyContainerAndStow`, which also moves the emptied container. |
| NeoForge client features on the mod bus (`RegisterKeyMappingsEvent`, `EntityRenderersEvent.RegisterRenderers`, `RenderGuiEvent.Post`) | These are NeoForge's client events; Fabric API's equivalents do not exist there. The HUD listener skips rendering when the GUI is hidden or no screen or player exists, as the Fabric hook does. |
| NeoForge mixins declared in a `[[mixins]]` block of `neoforge.mods.toml`, with a config but no refmap | NeoForge runs in the official namespace, so the compiled mixin references resolve as written; FML reads the block and applies the config. Proved in a development run on 2026-11-11 (server and client smoke, no attach error); the shipped jar is still to be checked. |
| NeoForge: the nested modules are added to each run's additional runtime class path (`RunModel.getAdditionalRuntimeClasspathConfiguration()`) | A development run loads the mod from its classes directory, so the jar-in-jar nested jars are absent. ModDevGradle leaves project dependencies off the launch class path and adds only the mods registered with `mods` to the mod folders, so without this the mod cannot see `modularity-api` (2026-11-11). ForgeGradle resolves the same dependency onto its run class path by itself. The shared layer needs no change for this: the mixins call the API as written once the API is on the run class path. |
| No guest mods on NeoForge | NeoForge has no public API to add a mod after discovery, and the candidate scan lives in `loom`, which NeoForge does not include. The NeoForge smoke runs without the guest and expects no guest lines. |
| NeoForge render data is the block entity of the position, read on the game thread only | NeoForge has no block view that snapshots render data for off-thread meshing; Fabric's block view reports the same object on the game thread. |
| Forge runs the launch stage in the mod constructor and the common stage on the first `RegisterEvent`, and takes its mod bus from `FMLJavaModLoadingContext` | Forge freezes its registries at the end of mod construction, as NeoForge does, and unfreezes them for the `RegisterEvent` pass only (states `UNFREEZE_DATA` then `LOAD_REGISTRIES`), so the common stage must run there. Forge injects only two constructor shapes into a `@Mod` class: no arguments, or one `FMLJavaModLoadingContext`; `FMLModContainer` looks for that type by name. The constructor keeps that shape and only adds the one-shot `RegisterEvent` listener (bean `modularity-4uol`). |
| Forge entity storages through `AttachCapabilitiesEvent<Entity>` with a `LazyOptional` provider per entity; fluids in millibuckets | Forge attaches a capability to an instance, not to an entity type, so every entity of an exposed type gets its own provider; `unitsPerMb()` is 1, as on NeoForge. Filling from the held item uses `FluidUtil.tryEmptyContainerAndStow`. |
| Forge payloads go through one `ChannelBuilder…payloadChannel()` channel, built at common setup | Forge fixes a channel when `build()` runs and has no registration event; the common stage is what collects the codecs, and common setup is the last point before any connection. A payload registered after the build throws. Receiving handlers run in `addMain`, on the game thread. |
| Forge client features split between the mod bus and `MinecraftForge.EVENT_BUS`; HUD through `AddGuiOverlayLayersEvent` | Key mappings, entity renderers, reload listeners and HUD layers are mod-bus events; the client tick and the capability events are game-bus events. The HUD layer goes above `ForgeLayeredDraw.HOTBAR` and skips rendering when the GUI is hidden or no screen or player exists. |
| Forge mixins declared in the jar manifest; one link still open | Forge 1.21.1's `mods.toml` has no `[[mixins]]` block and FML 52.1.21 contains no mixin code, so the config must come from Mixin itself. The route is the jar manifest (`MixinConfigs`), the one the Sponge Mixin wiki names for Forge (https://github.com/SpongePowered/Mixin/wiki/Mixins-on-Minecraft-Forge). Evidence from the 0.8.7 and ModLauncher jars: `Launcher.run` hands the GAME layer's `SecureJar`s to `LaunchPluginHandler.offerScanResultsToPlugins`, which calls `ILaunchPluginService.addResources` on the Mixin launch plugin; `MixinLaunchPluginLegacy` adds each jar as a `ContainerHandleModLauncher.Resource`, and `MixinPlatformAgentDefault.prepare()` reads `MixinConfigs` from that jar's manifest. The missing link is the jar to GAME layer step: FML's `ModDirTransformerDiscoverer` takes the regular `.jar` files in `mods/` whose module descriptor provides `cpw.mods.modlauncher.api.ITransformationService`, `net.minecraftforge.forgespi.locating.IModLocator` or `IDependencyLocator` (the `SERVICES` constant of that class). A Modularity jar provides none of them, so ModLauncher never offers it to Mixin. A development run takes another route: ForgeGradle adds a class folder, the discoverer visits files only, and Mixin reads the attribute from the `META-INF/MANIFEST.MF` of that folder (`MainAttributes.getDirAttributes`) instead. The Forge smokes of 2026-11-12 proved both halves: the run before the fix held `SpongePowered MIXIN Subsystem Version=0.8.7 … Service=ModLauncher` and no `Mixing` line, and the fourth pass, with the attribute in the generated metadata, holds seven `Mixing` lines (§7.10). Consequence: the Forge platform jar carries `MixinConfigs: <mod id>.forge.mixins.json`, its config sets `"verbose": true` so a successful application is visible, and `mods.toml` also declares the config so the name has one source. The jar now provides a locator service: `platforms/forge/.../forge/locating/ModularityForgeLocator` implements
`IDependencyLocator`, claims no mod, and is declared in `META-INF/services/net.minecraftforge.forgespi.locating.IDependencyLocator`,
so `ModDirTransformerDiscoverer` accepts the jar and Mixin can read the attribute. The locator route serves
the shipped jar alone, because ForgeGradle adds a class folder to a development run; the development route
is the folder manifest, which the fourth pass of 2026-11-12 proved (§7.10). A run from the shipped jar
stays open (bean `modularity-xbj0`). The file comes from the generated directory of `forgeLocatorService`
and only the `jar` task takes it. It must never sit in `src/main/resources`: the run of 2026-11-12 showed
that FML then reads the main output as a transformer artifact, so `modularity` vanished from the mod list
and the smoke failed with `Mod 'modularity_smoke' requires 'modularity' … not installed` (§7.10). References: the Mixin wiki above, https://mixins.microcontrollers.dev/general/pseudo/ for `@Pseudo` optional mixins, and https://github.com/MinecraftForge/coremods for FML 52's JavaScript coremods. Bean `modularity-6rm0` carries the next step. |
| Dynamic block models on the Forge family use `getModelData` and `getQuads` | Neither loader has the Fabric Renderer API. The section compiler calls `getModelData(level, pos, state, data)` for a position before it asks for quads, so both models resolve the emitted states there and return their quads from `getQuads`; `ModelEvent.ModifyBakingResult` swaps the model of every state of the block. The item form of such a block renders nothing, as on Fabric. |
| No guest mods on Forge | As on NeoForge: no public API to add a mod after discovery, and the candidate scan lives in `loom`. The Forge smoke runs without the guest and expects no guest lines. Epic `modularity-ecjq` owns the route after Stage 5. |
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
| NeoForge and Forge carry the shared mixins through a generated config | NeoForge declares it in `neoforge.mods.toml`; Forge names it in the jar manifest (`MixinConfigs`), because `mods.toml` has no mixin block. Both configs are generated by `GenerateModMetadataTask` in the official namespace, so neither needs a refmap. The Forge name reaches Mixin through the `MixinConfigs` attribute of the mod file: from the generated `META-INF/MANIFEST.MF` in a development run, and from the jar manifest, with the locator service (the row above), in a shipped jar. The fourth pass of 2026-11-12 proved the development half (§7.10). |
| NeoForge and Forge carry their own detection sources | Each loader layer installs a version source and a platform source from its own API; the shared runtime calls `VersionDetector` and `PlatformDetector` from `modularity-api`. |
| NeoForge entry points: three `@Mod` classes (`common`, `Dist.CLIENT`, `Dist.DEDICATED_SERVER`), each taking `IEventBus`; init runs in `enqueueWork` | This is the documented NeoForge sides pattern: the loader constructs only the class whose dist matches, so no client type reaches a dedicated server. `enqueueWork` runs the init on the main thread after the parallel setup phase. |
| `ModularModRegistry` lives in `modularity-api`, not in `platform` | `UniversalJar` relocates `platform.*`; an adapter of another mod links only against the unrelocated API. |
| NeoForge and Forge seam stubs throw `UnsupportedOperationException` naming the method instead of registering nothing | NeoForge and Forge register content before common setup; a silent no-op would hide a lost registration. The follow-up replays registrations (`modularity-d5hk` on NeoForge, `modularity-dti9` on Forge). |
| Fabric entrypoints found in the assembled sources, not listed in `platforms.toml`: a class that directly implements `ModInitializer`, `ClientModInitializer`, `DedicatedServerModInitializer` or `PreLaunchEntrypoint`. Roles are `common`, `client`, `server`, `launch`; Fabric's keys are `main`, `client`, `server`, `preLaunch`, Quilt's are `main`, `client`, `server`, `pre_launch` | The class says that it is an entrypoint, so a rename or a version layer that adds or removes one needs no table edit. Each loader defines its keys, so the role names are internal and the mapping happens once, when the metadata is written. Direct implementation only: a class that inherits an interface from a base class is not found; the same limit as the mixin scan. |
| Allure plugins 4.3.0 with the 3.x Java adapter (`allure-bom` 3.1.1) | One report shows every layer, and a test carries its layer as a label. The 3.x line is the current adapter; `allure-junit5` is 2.x only. The adapter is `compileOnly`/`runtimeOnly`, so the published API keeps no Allure dependency. |
| The report does not belong to `buildAll`, and the aggregate plugin's project links are cleared | The report downloads the Allure runtime, and the plugin's links would resolve the class path of every platform. `allureCollectResults` stages the raw results into `build/allure-results`, and the report task reads that directory. |

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
are gone; the same runs now expect the `(<side>) platform ...` line and the table stays as the
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

### Re-run after the mvd/mpd merge (2026-11-10)

`./gradlew -p modularity-gradle test` passes: 46 tests, no failure, including the seven `SmokeCheck` tests
that expect the capital-letter log lines.

`./gradlew -Pmodularity.platforms=quilt-21.1 :platforms:21.1:21.1-quilt:smokeServer` passes. The Quilt
server logged

    Entrypoint launch active (quilt)
    Entrypoint common active (quilt)
    (server) platform quilt:0.30.2, minecraft:1.21.1
    Modularity 0.1.0-SNAPSHOT on quilt, Minecraft 1.21.1: initializing 0 mod(s)
    Entrypoint server active (quilt)
    Mod modularity: loaded=true version=0.1.0-SNAPSHOT nested=no
    Server ticked 40 times, stopping

So the `quilt_loader` container reports the version in `gradle/platforms.toml`, and the smoke sees the
current line format and capital letters. The three Quilt entrypoints run (`launch`, `common`,
`server`), and the guest mod stays off, as docs/08 expects. An earlier run the same day reported the
same version in the line format of the time.

`./gradlew -Pmodularity.platforms=21.1 :platforms:21.1:jar` passes and writes the universal jar at
2026-11-10 13:01. `unzip -l` finds no `META-INF/services` entry in its 495 files, so the platform jars
in `platforms/*/versions/21.1/build/libs` now carry the current metadata.

### Re-run of the four loaders, 21.1 (2026-11-11)

`./gradlew -p modularity-gradle test` passes. It ran before the `SmokeCheck` change below; the smoke tasks of
the final pass compile the changed check, and its tests were checked with `javac` and a plain runner,
not by a second Gradle test run.

Server and client smoke pass for the four loaders of 21.1. Each `build/smoke/{server,client}.txt` holds
the platform line and, on the client, the world join, the screenshot and `Client done`:

    (server) platform fabric:0.20.1, minecraft:1.21.1
    (client) platform fabric:0.20.1, minecraft:1.21.1
    (server) platform quilt:0.30.2, minecraft:1.21.1
    (client) platform quilt:0.30.2, minecraft:1.21.1
    (server) platform neoforge:21.1.265, minecraft:1.21.1
    (client) platform neoforge:21.1.265, minecraft:1.21.1
    (server) platform forge:52.1.21, minecraft:1.21.1
    (client) platform forge:52.1.21, minecraft:1.21.1

The NeoForge run also proves that FML applies `modularity.neoforge.mixins.json`: the run reaches the
server and the client without an attach error. It does not prove the mixins from the shipped jar. These
runs predate the Forge wiring: at that date Forge named no config, and the Forge jar manifest has carried
`MixinConfigs` only since 2026-11-12 (7.8). The fourth pass of 2026-11-12 then showed the config applying
in a development run (7.10, PR-13). Every run loaded the mod from its classes directory (`nested=no`), so
the jar-in-jar packaging stays unproven.

The same pass ran again after the NeoForge run class-path fix, with the shared layer restored to its
committed form (`platforms/minecraft` byte-identical to `HEAD`) and with the strengthened `SmokeCheck`.
`Interceptors` and the two block mixins apply as written, the eight smokes pass, and each log holds the
entrypoint stage lines the check now expects: `Entrypoint launch active (<loader>)`,
`Entrypoint common active (<loader>)`, `Entrypoint mod-constructor active (<loader>)` on the Forge
family and the side stage line (§7.6).

### Feature smoke and the Forge-family common stage (2026-11-11, superseded)

`SmokeCheck` now also expects the two `Smoke features:` lines on every loader (§7.6), and the smoke mod is a
Modularity mod on every loader: Fabric and Quilt through the `"modularity"` entrypoint, NeoForge and Forge
through `Modularity.registerMod` in the smoke mod constructor. At that date the common stage of the Forge
family was moved to `NewRegistryEvent`. The run of 2026-11-12 (below) disproved that point and replaced it
with the first `RegisterEvent` (7.8); the two runs are the record of that change.

### Runs of 2026-11-12: the feature smoke and the dispatch point

`./gradlew -p modularity-gradle test` — BUILD SUCCESSFUL: 50 tests, 0 failures (`LayersTest` 11,
`ModularityVersionPluginTest` 4, `QuiltJarMetadataTest` 2, `SmokeCheckTest` 11, `TableAndMetadataTest` 9,
`UniversalJarTest` 5, `VersionsTest` 8).

After that run six tests were added: `UniversalJarTest.serviceFilesNameTheRelocatedClass` (the Forge
locator service, §7.8), `SmokeCheckTest.clientNeedsBothPayloadDirections`,
`SmokeCheckTest.dedicatedServerNeedsNoPayloadRoundTrip` (the network round trip, §7.6),
`SmokeCheckTest.storageLineMustAddUp` (the fluid unit) and
`SmokeCheckTest.clientFrameMustShowSomething`, `SmokeCheckTest.serverIgnoresTheScreenshot` (the rendered
frame). The run of 2026-11-12 (third pass, below) proves them.

### Third pass of 2026-11-12: the network, storage and frame checks

`./gradlew -p modularity-gradle test` — BUILD SUCCESSFUL, 57 tests, 0 failures (`LayersTest` 11,
`ModularityVersionPluginTest` 4, `QuiltJarMetadataTest` 2, `SmokeCheckTest` 17, `TableAndMetadataTest` 9,
`UniversalJarTest` 6, `VersionsTest` 8).

The eight 21.1 smokes then failed on the Forge server with none of the expected lines:

    net.minecraftforge.fml.LoadingFailedException: Loading errors encountered: [
        Mod 'modularity_smoke' requires 'modularity' 0 or above
    Currently, 'modularity' is not installed ]

Modularity itself was gone from the mod list. The cause was the locator service file that had just been
added to `platforms/forge/common/src/main/resources`: FML reads a class-path entry that names a locator
service as a transformer artifact, so the main output — which held `META-INF/mods.toml` — stopped being
read as a mod. The file now comes from the generated directory `build/generated/forge-locator` of the
`forgeLocatorService` task, and only the `jar` task takes it (`ForgePlatformSetup`); the development class
path never holds it. §7.8 records the decision.

With that fix the same eight smokes are green:

    ./gradlew -Pmodularity.platforms=fabric-21.1,quilt-21.1,neoforge-21.1,forge-21.1 \
      :platforms:21.1:21.1-fabric:smoke :platforms:21.1:21.1-quilt:smoke \
      :platforms:21.1:21.1-neoforge:smoke :platforms:21.1:21.1-forge:smoke
    BUILD SUCCESSFUL in 2m 58s

Each run holds the new lines. Fabric and Quilt report the droplet unit, the Forge family reports its own:

    Smoke storage: unitsPerMb=81 inserted=1000 units=81000 drained=400 amount=600
    Smoke storage: unitsPerMb=1 inserted=1000 units=1000 drained=400 amount=600

Every client run holds both network lines and a non-flat screenshot:

    Smoke network: server received 42
    Smoke network: client received 42

The client check now rejects a client task without the screenshot property, so these green runs prove
that `SmokeSetup` passed `<run dir>/screenshots/smoke.png` and that `SmokeCheck` read a rendered frame.

The first pass of the eight smokes (21.1, four loaders) failed on the two Forge-family loaders and passed
on Fabric and Quilt:

    missing log line /Smoke features: declared block=\S+ item=\S+ channel=\S+/
    missing log line /Smoke features: resolved server block=\S+ present=true/
    Execution failed for task ':platforms:21.1:21.1-neoforge:smokeServer'

The NeoForge server log showed the cause inside the `NewRegistryEvent` listener:

    java.lang.IllegalStateException: Registry is already frozen
        at net.minecraft.core.MappedRegistry.validateWrite(MappedRegistry.java:111)
        at net.minecraft.core.MappedRegistry.createIntrusiveHolder(MappedRegistry.java:390)
        at net.minecraft.world.level.block.Block.<init>(Block.java:67)
        at lv.cebbys.mcmods.modularity.platform.block.ModularBlock.<init>(ModularBlock.java:35)
        … SmokeFeatures.initialize(SmokeFeatures.java:43)

NeoForge `CommonModLoader` gives the order: `RegistryManager.postNewRegistryEvent()`, then
`GameData.unfreezeData()`, then `GameData.postRegisterEvents()`. Every vanilla registry is therefore still
frozen during `NewRegistryEvent`, and an eagerly constructed `Block` cannot be made there. Forge showed
the same failure.

The second pass moved the common stage to the first `RegisterEvent` (§7.8) and ran the same eight smokes:

    ./gradlew -Pmodularity.platforms=fabric-21.1,quilt-21.1,neoforge-21.1,forge-21.1 \
      :platforms:21.1:21.1-fabric:smoke :platforms:21.1:21.1-quilt:smoke \
      :platforms:21.1:21.1-neoforge:smoke :platforms:21.1:21.1-forge:smoke
    BUILD SUCCESSFUL in 4m 14s

All eight `smokeServer` / `smokeClient` tasks ran and the check passed for each. Every `build/smoke/server.log`
and `client.log` of the four loaders holds both feature lines, for example:

    Smoke features: declared block=modularity_smoke:feature_block item=modularity_smoke:feature_item channel=modularity_smoke:feature
    Smoke features: resolved server block=modularity_smoke:feature_block present=true
    Smoke features: resolved client block=modularity_smoke:feature_block present=true

The Forge-family logs hold the stage order, with the common stage after the constructor and the platform
line:

    Entrypoint mod-constructor active (neoforge)
    Entrypoint launch active (neoforge)
    Entrypoint common active (neoforge)
    (server) platform neoforge:21.1.265, minecraft:1.21.1
    Modularity 0.1.0-SNAPSHOT on neoforge, Minecraft 1.21.1: initializing 1 mod(s)
    Smoke features: declared …

This proves: the two Forge-family loaders accept a dependent mod's registration in its own constructor, the
common stage runs once in the writable registry window, and a block, an item and a payload channel
registered through the API resolve on the server and on the client on all four loaders. It does not prove
the shipped jar: every run loaded the mod from its classes directory (`nested=no`).

### Fourth pass of 2026-11-12: the client seam and the Forge mixins

The client smoke of the third pass ran on Fabric and Quilt only for the new lines. On the Forge family it
crashed with `NullPointerException … "entityrenderer" is null` at `EntityRenderDispatcher.shouldRender`,
because the client stage ran on `FMLClientSetupEvent`, after the client registration events. §7.8 records
the decision: the client stage now runs at the end of the common stage, inside the first `RegisterEvent`,
and every client registration keeps an `Id` that the loader resolves in its own event.

The first run of that fix (10:46) left the Fabric, Quilt and NeoForge client runs green and failed only the
Forge client, which missed two lines:

    missing log line /Smoke client: hud drawn \d+x\d+/
    missing log line /Smoke particles: hit \S+ at \S+/

The log named both causes. `ForgeLayeredDraw` logged `Expected layer minecraft:hotbar was not found in
stack minecraft:vanilla_root, no layer modifications have been made`, so the HUD layer was added with
`addAbove` against an anchor that Forge 52 does not have; it is now appended with
`ForgeLayeredDraw.add(id, layer)`. The log also held no `Mixing` line at all: a Forge development run reads
the manifest of its classes folder, not of the jar, so the `MixinConfigs` attribute of the jar never
reached Mixin, and the hit-particle mixin (`ParticleEngineMixin`) never applied. The generated metadata now
carries the same attribute for Forge, and the jar task excludes that copy (§7.8).

With both fixes the same eight smokes are green:

    ./gradlew -Pmodularity.platforms=fabric-21.1,quilt-21.1,neoforge-21.1,forge-21.1 \
      :platforms:21.1:21.1-fabric:smoke :platforms:21.1:21.1-quilt:smoke \
      :platforms:21.1:21.1-neoforge:smoke :platforms:21.1:21.1-forge:smoke
    BUILD SUCCESSFUL in 2m 58s

Every client log holds the six client lines of §7.6, the screenshot is saved, and the Forge client log
holds seven `Mixing … from modularity.forge.mixins.json` lines:

    Mixing BlockStateBaseMixin from modularity.forge.mixins.json into …BlockBehaviour$BlockStateBase
    Mixing EntityAccessor from modularity.forge.mixins.json into net.minecraft.world.entity.Entity
    Mixing LevelMixin … into net.minecraft.world.level.Level
    Mixing ServerLevelMixin … into net.minecraft.server.level.ServerLevel
    Mixing BlockItemMixin … into net.minecraft.world.item.BlockItem
    Mixing client.ParticleEngineMixin … into net.minecraft.client.particle.ParticleEngine
    Mixing client.CameraMixin … into net.minecraft.client.Camera

The same pass re-ran the build contract and rebuilt the jars:

    ./gradlew -p modularity-gradle test
    BUILD SUCCESSFUL in 3s

    ./gradlew -Pmodularity.platforms=fabric-21.1,quilt-21.1,neoforge-21.1,forge-21.1 \
      :platforms:21.1:21.1-forge:build :platforms:21.1:jar
    BUILD SUCCESSFUL in 4s

The modularity-gradle results hold 59 tests and 0 failures.

The Forge jar holds one `META-INF/MANIFEST.MF` of 69 bytes with `MixinConfigs: modularity.forge.mixins.json`
and `modularity.forge.mixins.json`; the copy in the generated metadata does not reach the jar (§7.8). The
universal jar is 1,142,719 bytes.

This proves: the client stage of the two Forge-family loaders runs inside the registry pass and its
registrations reach the game; the dynamic model, the hit particles, the HUD layer, the key mapping and the
entity renderer all run on the four loaders; and Mixin applies Modularity's Forge config in a development
run. It does not prove the config from the shipped Forge jar (no run starts from a jar), and the universal
jar still needs a loader-conditional `MixinConfigs` (bean `modularity-xbj0`). The Forge client log also
holds one suppressed error, `Failed to handle packet … ServerboundPlayerActionPacket … IllegalStateException:
Can not retrieve LootModifierManager until resources have loaded once.`; the block still broke and the
scenario completed, so it is recorded as an open note (TODO-NOW.md).

### Fifth pass of 2026-11-12: the OBJ model path

The port list names OBJ models as part of the client area, and no run had ever used them. The path turned
out to be shared code with no anvil copy needed (7.8): only the Fabric-flavoured reload wrapper is
loom-only. The smoke now proves it on every loader with two lines:

    Smoke obj: parsed modularity_smoke:models/obj/smoke_cube.obj faces=6 groups=1
    Smoke obj: drawn modularity_smoke:feature_entity faces=6

The first run of the change parsed the model and was green on all four loaders, but drew nothing, so the
frame held no object. The parse-only line proved the resource and the parser, not the rendering. The
entity renderer now draws the parsed cube with `RenderOutput.model` and a vanilla texture, centred on the
spawned entity, and the check requires both the parse and the draw:

    ./gradlew -p modularity-gradle test
    BUILD SUCCESSFUL in 3s

    ./gradlew -Pmodularity.platforms=fabric-21.1,quilt-21.1,neoforge-21.1,forge-21.1 \
      :platforms:21.1:21.1-fabric:smoke :platforms:21.1:21.1-quilt:smoke \
      :platforms:21.1:21.1-neoforge:smoke :platforms:21.1:21.1-forge:smoke
    BUILD SUCCESSFUL in 2m 19s

The modularity-gradle results hold 59 tests and 0 failures.

Every client log holds both lines, and every screenshot shows the stone-textured cube in front of the
player. The OBJ is a unit cube with per-face UVs; the first version carried no UVs, so it sampled one
texel and read as a flat grey square.

This proves: the client resource manager, the reload hook, the parser and the geometry sink of all four
loaders. It does not prove an OBJ model in a shipped jar, and it does not prove a model that a mod ships
with its own texture.

### Sixth pass of 2026-11-12: the storage seam in the game

`StorageBackend` has three methods. The first, `unitsPerMb`, was already proved. The other two had never been
called by a run, so the four storage task beans stayed open. The smoke now proves both, and the first
attempts found four defects in the loader code:

1. Quilt Loader refused the smoke mod: the `modularity` entrypoint class named client vanilla packages and
   the loader checks that in `pre_launch` (`Cannot load package net.minecraft.client.server because
   Mismatched @Environment`). The client-only code moved into `SmokeScenario` (7.8).
2. Fabric `ModularityLoomTankStorage` passed a null transaction into `SnapshotParticipant`, which throws.
3. Forge rejected `addListener` with the generic `AttachCapabilitiesEvent`; it needs `addGenericListener`.
4. Forge moved no fluid from a held bucket and kept the full bucket in the hand.

With those fixed, the four 21.1 smoke runs are green and every client log holds both lines:

    Smoke storage: held item moved 1000 mB
    Smoke storage: exposed 500 mB

    ./gradlew -Pmodularity.platforms=fabric-21.1,quilt-21.1,neoforge-21.1,forge-21.1 \
      :platforms:21.1:21.1-fabric:smoke :platforms:21.1:21.1-quilt:smoke \
      :platforms:21.1:21.1-neoforge:smoke :platforms:21.1:21.1-forge:smoke
    BUILD SUCCESSFUL in 2m 53s

    ./gradlew -p modularity-gradle test
    BUILD SUCCESSFUL in 3s

The modularity-gradle results hold 59 tests and 0 failures.

The Forge screenshot shows the emptied bucket in the first hotbar slot, which proves that the item moved as
well as the fluid. The stone cube of the OBJ model now fills exactly the space of the block that was mined:
the entity stands in the block centre, so the renderer offsets the cube by half a block on every axis.

This proves: the fill from the held item and the exposure of the entity tank through the lookup of all four
loaders, in the game. It does not prove either method in a shipped jar, and it does not prove a second mod
reading the tank: the smoke asks the loader lookup from the same mod that registered it, which is the same
API call another mod makes but not another mod.

### Seventh pass of 2026-11-12: the storage cycle and the fourth seam method

The three-click cycle (7.6) first ran through the loader lookups and caught a defect in the two Forge-family
tank handlers: they read `tank.nativeFluid()` after emptying the tank, so a drain that worked reported
`0 mB`. With that fixed the cycle passed on all four loaders.

The owner then asked for the take direction in the seam, so `StorageBackend.drainToHeldItem` now exists and
the smoke uses it instead of its own probe. Two further defects came out of the move:

- NeoForge's backend used `IFluidHandler.FluidAction` and `FluidStack` without their imports, so the
  platform did not compile;
- on Quilt the item swap of a Fabric drain threw `Transfer variant may not be blank`, because Quilt's water
  fluid reports no bucket item, and the throw rolled the whole drain back. The move now commits on its own
  and the swap is guarded.

Final run, all four loaders, every client log with the same six lines:

    Smoke storage: held item moved 1000 mB
    Smoke storage: entity tank holds 1000 mB
    Smoke storage: bucket took 1000 mB
    Smoke storage: bucket took 0 mB
    Smoke storage: exposed 500 mB

    ./gradlew -Pmodularity.platforms=fabric-21.1,quilt-21.1,neoforge-21.1,forge-21.1 \
      :platforms:21.1:21.1-fabric:smoke :platforms:21.1:21.1-quilt:smoke \
      :platforms:21.1:21.1-neoforge:smoke :platforms:21.1:21.1-forge:smoke
    BUILD SUCCESSFUL in 3m 28s

    ./gradlew -p modularity-gradle test
    BUILD SUCCESSFUL in 3s

The modularity-gradle results hold 59 tests and 0 failures.

This proves the full fluid cycle on an entity tank, through player actions, on all four loaders: fill from a
bucket, take it out, and refuse a second empty bucket. It does not prove a modded container item, and it
does not prove the cycle in a shipped jar.

### The shipped jars, 2026-11-12

The same pass built the four per-loader jars and the universal jar. Both commands are green:

    ./gradlew -Pmodularity.platforms=fabric-21.1,quilt-21.1,neoforge-21.1,forge-21.1 \
      :platforms:21.1:21.1-fabric:build :platforms:21.1:21.1-quilt:build \
      :platforms:21.1:21.1-neoforge:build :platforms:21.1:21.1-forge:build
    BUILD SUCCESSFUL in 9s

    ./gradlew -Pmodularity.platforms=fabric-21.1,quilt-21.1,neoforge-21.1,forge-21.1 \
      :platforms:21.1:jar
    BUILD SUCCESSFUL in 2s

The per-loader jars are `platforms/<loader>/versions/21.1/build/libs/modularity-<loader>-21.1-0.1.0-SNAPSHOT.jar`.
The universal jar is `platforms/versions/21.1/build/libs/modularity-1.21.1-0.1.0-SNAPSHOT.jar`, 1,141,046
bytes, and it holds:

    META-INF/MANIFEST.MF                                  (22 bytes: "Manifest-Version: 1.0")
    fabric.mod.json      mixins: ["modularity.mixins.json"]
    quilt.mod.json       mixin:  "modularity.quilt.mixins.json"
    META-INF/mods.toml   [[mixins]] config="modularity.forge.mixins.json"
    META-INF/neoforge.mods.toml  [[mixins]] config="modularity.neoforge.mixins.json"
    modularity.mixins.json  modularity.quilt.mixins.json
    modularity.neoforge.mixins.json  modularity.forge.mixins.json
    META-INF/jars/modularity-api-0.1.0-SNAPSHOT.jar          (Fabric and Quilt jar-in-jar)
    META-INF/jarjar/lv.cebbys.mcmods.modularity-api-0.1.0-SNAPSHOT.jar
    META-INF/jarjar/metadata.json                            (Forge family jar-in-jar)

Every metadata file names its entrypoints under the relocated package
`lv.cebbys.mcmods.modularity.universal.<loader>.<loader>.entrypoint`, which is the `UniversalJar`
relocation at work. The universal manifest carries no `MixinConfigs` attribute: one attribute cannot
name a different config for four loader trees. The per-loader Forge jar does carry
`MixinConfigs: modularity.forge.mixins.json`; no run starts from a jar, so Mixin reading it from the jar
stays unproven (7.8). A development run applies the same config through the manifest of the generated
metadata (7.10).

This listing comes from the run of 2026-11-12 01:44, after the Forge config and its `[[mixins]]` block
were added. The earlier listing of the same day holds three configs and no block. A later change adds one
entry that this listing does not show yet: `META-INF/services/net.minecraftforge.forgespi.locating.IDependencyLocator`,
which names the relocated `ModularityForgeLocator` (7.8). The `forgeLocatorService` task now generates
that file for the Forge `jar` task, and `UniversalJar` rewrites it. The jar of 2026-11-12 predates the
task, so the next `:platforms:<version>:jar` run must confirm the entry.

This proves packaging: the four loaders' metadata, the four mixin configs and both nested-API routes
reach one jar. It does not prove a load from the jar: no smoke task starts the game from a jar, so a
shipped-jar load stays open (TODO-NOW.md, `PORT-REVIEW.md`). The one jar carries one `MixinConfigs`
attribute for four loader trees, so the universal jar still needs a loader-conditional registration
(bean `modularity-xbj0`).



## 7.11 Test pyramid report (Allure)

One report shows every layer. Each stage of the plan adds one layer; S1 (L1) is wired now.

| Layer | Producer | Written as |
|---|---|---|
| L1 unit tests | `:modularity-api:test`, `:modularity-testkit:test`, `-p modularity-gradle test` | Allure results from the JUnit 5 engine, through the `allure-jupiter` adapter |
| L2 build contract | the modularity-gradle tests (`LayersTest`, `TableAndMetadataTest`, …) | the same engine, inside the included build |
| L3 in-game smoke | `smokeServer` / `smokeClient` per platform | one result file per run, with the log, the summary and the screenshot attached |
| L4 production jar | `modularity-xbj0` | not yet |

Tasks:

- `allureCollectResults` — `Sync` into `build/allure-results`. It copies the results of the two library
  modules and of the included build, and depends on their `test` tasks. `-Pmodularity.allure.tests=false` copies without running a
  test, which the CI jobs use after they have run the tests themselves. `executor.json` is left out: every
  project writes one and the report does not read it.
- `allureAggregateReport` — writes `build/reports/allure-report/allureAggregateReport/`. The aggregate
  plugin links itself to every project of the build; the root build clears those links, because resolving
  them would set up the class path of every platform. The task reads `build/allure-results` instead.
  It always runs (`upToDateWhen { false }`): the plugin reads the results directory in the task body and
  does not declare it as an input, so Gradle would keep an old report after the results change.
  The task sets `clean = true`. The Allure 3 CLI writes nothing and exits with 0 when the output
  directory already exists, so a run without the clean step leaves the first report in place.
  The task also passes `allurerc.json` as its config file: it groups the tree by epic, feature, story and
  functionality. The plugin's own config has no grouping, and the tree then follows the Java package path
  (`lv > cebbys > mcmods > ...`).
  Open the report through an HTTP server (`allureAggregateServe`, or any static file server); a `file://`
  URL does not load it, because the page fetches its data and the browser blocks that request.
- Each `test` task deletes its own `build/allure-results` first. The adapter adds to that directory and
  never removes old files, so a second run would leave two results per test.

Labels: the tree has four levels, and one test carries all four.

| Level | Label | Value |
|---|---|---|
| 1 | `epic` | `Modularity` |
| 2 | `feature` | the layer: `Unit Tests`, `Integration Tests`, `End to End Tests`, `Production Jar` |
| 3 | `story` | the API concern: `Version Model API`, `Core API`, `Platform Detection`, `Entity API`, ... |
| 4 | `functionality` | the behaviour: `Minecraft version parsing`, `Source ordering`, `Yaw rotation`, ... |

A test also carries the requirement id as `issue` and `tag`. The feature label is the filter that turns one
report into the pyramid. Every test has a `@DisplayName` sentence, so the tree shows a sentence instead of
a method name.

The report sorts each level by name: `allurerc.json` sets `defaultSortBy` to `name,asc`. Allure's other
sort mode, `order`, follows the order in which the report reads the result files, so two runs can show
two different orders. The feature values carry no layer number, so the tree is alphabetical:
`End to End Tests`, `Integration Tests`, `Production Jar`, `Unit Tests`.

A behaviour gets a `functionality` label only when two or more tests prove it. A behaviour that one test
proves has no label, so that test sits directly under its story and no group in the tree holds one test.
The story is the concern that owns the behaviour; do not split one concern over two stories.

Allure has no annotation for a custom label name. `@Functionality` in the test fixtures is therefore a
meta-annotation of Allure's `LabelAnnotation`, and it writes the label `functionality`. The report groups by
that label name, so the name in `Functionality.LABEL` and the name in `allurerc.json` must match.

The values live in `ModularityAllureConstants`, published as test fixtures of `modularity-api`: one child
class per label (`Epics`, `Features`, `Stories`, `Functionalities`, `Issues`, `Tags`), so a test names a
constant instead of a string. The included build cannot consume them, so `modularity-gradle` keeps its own copy,
`BuildAllureLabels`, with the same values and the same rule. The two copies must agree, or the two layers
split in the report.

L2 uses the same four levels. `Integration Tests` is the feature. The stories are the concerns of the
build: `Layer Assembly`, `Platform Table and Metadata`, `Universal Jar`, `Version Plugin Tasks` and
`Quilt Jar Metadata`. Two classes of the included build test plain Java, so they are `Unit Tests`:
`Version Ranges` (`VersionsTest`) and `Smoke Log Checks` (`SmokeCheckTest`).

Steps: each test calls small helper methods that carry `@Step`. Each helper does one action, or one check,
and Allure writes it under the test. A helper takes only text, numbers and values with a readable text
form as parameters, because Allure writes every parameter into the report; an object without a text form
would show as `lv.example.Codec$1@308a9264`. The objects a test creates are fields of the test instance.
The adapter sets `aspectjWeaver = true`, which adds
`-javaagent:aspectjweaver` to the test JVM; without the weaver, the `@Step` annotation has no effect. The
test fixtures name `allure-java-commons` as `testFixturesCompileOnly`, because the adapter adds Allure to
the test configurations only.

An `issue` value is a pull-request number, not a label. `allure.properties` in the test resources of each
module sets `allure.link.issue.pattern=https://github.com/CebbysMods/modularity/pull/{}`, and the adapter
applies it at test time. A link with no URL is dropped by the report, so the pattern is required for the
pull-request link to appear. The `tag` label stays readable (`PR-4`) and filters the report.

The included build is not a project of this build, so its results reach the report only through
`allureCollectResults`: the task copies `modularity-gradle/build/allure-results` as a plain path and depends on
`gradle.includedBuild('modularity-gradle').task(':test')`. The aggregate plugin does not discover an included
build, so a missing copy step leaves L2 out of the report without an error. `modularity-gradle` declares the
adapter plugin itself, with the version in its own `gradle.properties`, and keeps the AspectJ weaver off,
because no L2 test writes a `@Step` helper.

CI: `build.yml` has a `report` job that runs both test sets, writes the report and uploads
`allure-report-build`; it fails last, so a red test still publishes the report. `smoke.yml` publishes
`smoke-allure-report` (stage S3).

Status: L1 is wired and was run on 2026-11-10: `allureAggregateReport` wrote a report of 50 tests, all
passed, from `modularity-api` (28) and `modularity-testkit` (22), with a pull-request link on each test.
The same day the tests gained the four-level labels, the `@DisplayName` sentences and the `@Step` helpers.
The test run of 2026-11-10 17:34 passed (50 tests) and wrote 176 steps, so the AspectJ weaver works with
`@Step`. The report run of 2026-11-10 17:53 failed because the HTTP server held the report directory; the
report was then written by the bundled Allure CLI. The report showed two defects: two stories owned the
version tests (`Core API` and `Version API`), and most behaviour groups held one test. The tests were
regrouped after that run: `Version Model API` replaces `Version API`, the version tests of `CoreApiTest`
moved to it, and a behaviour keeps its `functionality` label only when two or more tests prove it. The
step helpers no longer take objects as parameters. The regroup was then run on 2026-11-10: both test tasks
passed (50 tests) and `allureAggregateReport` wrote the report. The tree holds no group with one test, and
no step parameter shows an object identity string.

S2 was run on 2026-11-10: `./gradlew -p modularity-gradle test` passed (46 tests) and wrote
`modularity-gradle/build/allure-results`; `./gradlew -Pmodularity.platforms=none allureAggregateReport` then
wrote one report of 96 tests: `Unit Tests` 65 (the 50 module tests and 15 modularity-gradle tests) and
`Integration Tests` 31. The L2 tree holds the five stories `Layer Assembly` (layer errors 6, layer
precedence 3, 2 direct), `Platform Table and Metadata` (generated metadata 4, source scan 2, 3 direct),
`Universal Jar` (relocation 3, loader trees in the jar 2), `Version Plugin Tasks` (task graph 3,
1 direct) and `Quilt Jar Metadata` (nested jar wrapping 2); the L1 tree holds `Version Ranges` 8 and
`Smoke Log Checks` 7 in the same report. No group holds one test. L3 is stage S3 of the bean epic.
