# 4. Milestones

Each milestone ends with a **reference mod** that proves the new API surface on real loaders. A
milestone is done when its reference mod's feature tests pass on the testkit **and** the jars build
and run in-game on every platform of that milestone.

## M1 – Core API + Refabslab parity (this branch)

Goal: research, design, and enough API to reimplement refabslab with zero Minecraft code in the mod.

* [x] Research of all repositories ([01](01-research.md)), requirements ([02](02-requirements.md)), design ([03](03-api-design.md))
* [x] `modularity-api`: ids, versions, `Versioned`, handles, blocks, properties, behaviours, block data, interceptors, dynamic models, hit particles
* [x] `modularity-testkit`: in-memory game for unit tests
* [x] `platforms/fabric-21.1`
* [x] refabslab ported: `refabslab-common` (pure API) + `refabslab-fabric-1.21.1` (metadata + resources only)
* [x] Feature parity tests for every refabslab feature (19 tests) + API tests
* [x] CI for both repositories
* [ ] In-game verification on Fabric 1.21.1 (with and without Sodium/Iris) – manual, needs a client
* [ ] Publish artifacts to a working maven (GitHub Packages) – the Heroku maven is gone

## M1.5 – Entities and client APIs for the vehicle mod (branch `feature/entities-and-client-apis`)

Pulled forward from M3/M4 because the vehicle mod (CebbysMods/immersive-vehicles V1) needs them.

* [x] Entities: `EntityDefinition` (size, tracking, seats, typed `EntityData` with saved/synced flags, fluid tank, inventory), `EntityBehaviour` (tick, interact, attacked)
* [x] Items: `ItemDefinition`, `ItemBehaviour#useOnBlock`, creative tabs
* [x] `Level#blockCollisions` (single terrain query), `spawn`, item drops
* [x] Typed client/server `Channel<T>`
* [x] Transfer: `FluidTank` / `ItemInventory`, exposed through the Fabric Transfer API (refuel from any fluid container; `modularity:fluid_storage` / `modularity:item_storage` entity lookups)
* [x] Client: key bindings, movement input, tick hook, camera controller, HUD layers, looping entity sounds, entity renderers with OBJ models
* [x] Testkit: ticking, riding, save/reload, loopback channels, client simulation
* [x] Reference: vehicle mod V1 (drivable sedan) incl. in-game smoke test
* [x] Orientation and multipart entities (vehicle refactor): `Rotation` (quaternion), `EntityRef#orientation` synced and interpolated, seats follow it, `RenderOutput#rotate`; `EntityPart` boxes per entity (Ender Dragon style part entities with consecutive ids), aimed at / hit / used per part (`EntityBehaviour#interactPart` / `#attackedPart`), solid parts collide with their rotated shape (column approximation) and carry entities standing on them
* Not yet: menus/screens, entity data on vanilla entities, NeoForge implementation

## M1.6 – Utility mods mvd and mpd (branch `feature/mvd-mpd-utilities`)

Version and platform detection as two separate mods next to Modularity ([06](06-utility-mods.md)).

* [x] Gradle skeleton under `utilities/`
* [x] mvd (`VersionDetector`), clean copy and refactor of minecraft-version-loader
* [x] mpd (`PlatformDetector`)
* [x] Fabric 1.21.1 wrappers, Modularity depends on both, `FabricRuntime` uses them
* [x] Smoke test: 3 separate mods in game
* [x] Docs updated to what was built
* Not yet: backports to older Minecraft versions, NeoForge/Forge wrappers, own repositories

## M2 – Multi-version Fabric (branch `feature/multi-version-fabric`)

Goal: one Fabric platform jar per Minecraft version from 1.21.1 to the latest stable (26.3), built
from layered code by our own Gradle plugin ([07](07-multi-version-build.md)).

* [x] Design doc and version table
* [x] Build upgrade: Loom 1.19 (remap + non-remap plugins), Gradle 9.8, Gradle on JDK 25
* [x] Plugin `lv.cebbys.modularity` in `build-logic/`: version table, one project per platform, layered sources, override checks, generated `fabric.mod.json` and mixin config, `buildAll`/`collectJars`/`smokeAll`
* [x] `platforms/fabric-21.1` moved into `platforms/fabric/common` + `versions/21.1`
* [x] Ports 21.2 … 21.11, 26.1 … 26.3, one version at a time (port log in [07](07-multi-version-build.md#79-port-log))
* [x] mvd and mpd: one jar each for every version (`minecraft >=1.21.1`, built against 1.21.1, verified on 1.21.1 … 26.3)
* [x] Smoke tests per platform (server for all, client for 21.1, 21.6, 21.11, 26.1, 26.3)
* [x] Guest mods ([08](08-guest-mods.md)): one jar with `modularity.mod.json` and no loader metadata becomes a Fabric mod at startup; smoke-tested on every platform
* [x] Loaders in the build: platforms `<loader>-<version>`, layer root stacks (`platforms/minecraft` + loader); Quilt 1.21.1 (the Fabric jar on Quilt Loader), NeoForge 1.21.1 (ModDevGradle), Forge 1.21.1 (ForgeGradle 7): Modularity, mvd and mpd load, server and client smoke pass
* [ ] Shared `platforms/minecraft` layer: plain Minecraft code moved out of `platforms/fabric`
* [ ] NeoForge and Forge 1.21.1 runtime: refabslab's feature set, then the rest of the API
* [ ] Guest mods on Quilt, NeoForge and Forge

## M2b – Second loader and build tooling for mods

* `platforms/neoforge/…` with the same layering (block entity `ModelData` for render data, `DeferredRegister` behind `BlockHandle`, generated `@Mod` adapter); Forge and Quilt later
* Plugin `lv.cebbys.modularity` for mods built on Modularity: declares targets, generates `fabric.mod.json`/`neoforge.mods.toml`, builds one jar per target, runs testkit tests once
* Per-target optional native source sets + `Versioned` wiring
* Version-aware resources (e.g. `tags/blocks` → `tags/block` rename in 1.21)
* Reference: refabslab on several targets from one source

## M3 – Items, interaction, events, networking, config

* Items beyond M1.5: tools/armor/food, item behaviours (use, use on entity)
* More block hooks: use, neighbour updates, ticking, shapes, random ticks; block entities with tickers
* Event bus: lifecycle, server/level tick, player interaction, block break/place, commands
* Typed networking (`mod.channel(...)`) with `DataCodec`
* Typed config with client sync
* Reference: a small "utility blocks" mod (e.g. cedyte) – to be defined with the author

## M4 – Entities and rendering

* Entity types, attributes, spawning, projectiles, riding/vehicles, synced entity data
* Entity & block entity renderers, render layers, tints, key bindings, input
* Optional GeckoLib integration module
* Reference: mythos-odyssey spells (projectile + animated model) and immersive-vehicles basics

## M5 – Data and resources

* Runtime asset/data packs (respro rebuilt on Modularity), data generation from the same definitions
* Recipes, loot tables, tags, advancements, worldgen features
* Reference: respro as a Modularity library; refabslab resources generated

## M6 – UI

* Menus + container screens, plain screens, HUD overlays, tooltips

## M7 – Breadth

* Fluids, dimensions, enchantments, effects, sounds, particle types, attachments on entities/chunks/levels
* Compat modules (recipe viewers, Jade), Quilt and (optionally) Forge platforms, older versions on demand
