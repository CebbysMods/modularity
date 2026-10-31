# 4. Milestones

Each milestone ends with a **reference mod** that proves the new API surface on real loaders. A
milestone is done when its reference mod's feature tests pass on the testkit **and** the jars build
and run in-game on every platform of that milestone.

## M1 – Core API + Refabslab parity (this branch)

Goal: research, design, and enough API to reimplement refabslab with zero Minecraft code in the mod.

* [x] Research of all repositories ([01](01-research.md)), requirements ([02](02-requirements.md)), design ([03](03-api-design.md))
* [x] `modularity-api`: ids, versions, `Versioned`, handles, blocks, properties, behaviours, block data, interceptors, dynamic models, hit particles
* [x] `modularity-testkit`: in-memory game for unit tests
* [x] `platforms/fabric-1.21.1`
* [x] refabslab ported: `refabslab-common` (pure API) + `refabslab-fabric-1.21.1` (metadata + resources only)
* [x] Feature parity tests for every refabslab feature (19 tests) + API tests
* [x] CI for both repositories
* [ ] In-game verification on Fabric 1.21.1 (with and without Sodium/Iris) – manual, needs a client
* [ ] Publish artifacts to a working maven (GitHub Packages) – the Heroku maven is gone

## M2 – Second loader, second version, build tooling

Goal: prove "multi loader, multi version" with the same refabslab common jar.

* `platforms/neoforge-1.21.1` (block entity `ModelData` for render data, `DeferredRegister` behind `BlockHandle`, generated `@Mod` adapter)
* `platforms/fabric-1.20.1` (Java 17, older model API) → extract shared code into `platforms/common-*`
* Gradle plugin `lv.cebbys.modularity`: declares targets (`fabric("1.21.1")`, `neoforge("1.21.1")`), generates `fabric.mod.json`/`neoforge.mods.toml`, builds one jar per target, runs testkit tests once
* Per-target optional native source sets + `Versioned` wiring
* Version-aware resources (e.g. `tags/blocks` → `tags/block` rename in 1.21)
* Reference: refabslab on 3 targets from one source

## M3 – Items, interaction, events, networking, config

* Items (`mod.item`), tools/armor/food, creative tabs, item behaviours (use, use on block/entity)
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
