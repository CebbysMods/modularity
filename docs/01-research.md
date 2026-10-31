# 1. Research: current state of the CebbysMods repositories

Snapshot taken 2026-10-31 while starting milestone 1.

## Overview

| Repository | Loader / MC | State | Relevance for Modularity |
|---|---|---|---|
| `modularity` | – | Only a README ("template for multi version/loader modding") | Target of this work |
| `refabslab` | Fabric 1.16 → 1.19 (branches), main = 1.18.2 | Released mod, `release/1.0` has the latest features, `feature/mod-modularity-...` is an early multi-version experiment | **Milestone 1 reference mod** |
| `respro` | Fabric 1.16 → 1.19 | Released library: runtime resource pack generation (blockstates, models, recipes, KleeSlabs compat) | Requirement source for the resources milestone |
| `mythos-odyssey` | NeoForge 1.21 | Template + `feature/base-for-magic-spells` (spell projectile entity, GeckoLib model) | Requirement source: entities, renderers, 3rd party libs |
| `immersive-vehicles` | Fabric 1.21 | Unmodified Fabric template | Expected requirements: entities, input, networking, rendering |
| `cedyte` | Fabric 1.20.2 | Unmodified Fabric template | No requirements yet |

## refabslab in detail

### Features (from `VERSIONS.md` and the `release/1.0` code)

1. **Combine two different slabs** in one block space. Implemented by a mixin into `SlabBlock#getStateForPlacement` and `SlabBlock#canBeReplaced`.
2. **Double slab block** with an `IntegerProperty light_level (0..15)` = brightest half; piston reaction `BLOCK`.
3. **Per-position data** (which top/bottom slab) stored in a Cardinal Components **chunk component**, auto-synced to clients. (0.6.1 moved away from block entities to this.)
4. **Mining**: hardness = harder slab, speed = best tool speed, harvest penalty `100 - 35 per correctly-tooled slab`.
5. **Breaking**: drops both slabs if either half is harvestable; break particles/sounds for both halves; anger piglins for guarded blocks; game event.
6. **Hit particles**: texture of the half being hit (mixin into `ParticleManager`).
7. **Rendering**: custom baked model that merges the two slab models (Fabric Renderer API), cached per combination. Works with Sodium + Indium and Iris (0.7.0).
8. **Render-thread data access**: a `ChunkLocator` registry that digs the chunk out of `World`, `ChunkRendererRegion` (reflection) and Sodium's `WorldSlice` (reflection).
9. **Compatibility**: Sodium requires Indium (crash with message), Minegate `grass_block_slab` blacklisted, KleeSlabs compat resources (backup).
10. **Resources** generated at runtime through respro (blockstate with 16 light-level variants pointing to the dynamic model).
11. **Bug fixes worth keeping as tests**: falling sand on a lower slab crashed; creative placement consumed items; slabs replaced by any block.

### Multi-version attempts

* `release/1.0`: one jar per version, mixins moved to a `bridge/1.19` module and selected at runtime by a `IMixinConfigPlugin` that reads the Minecraft version through reflection (`MinecraftVersion`).
* `feature/mod-modularity-to-support-multiple-minecraft-versions`: modules `api` / `core` / `bridge/<loader>/<version>` / `loader/<loader>`. Bridges contribute *plugins* annotated with `@RefabslabPluginIdentifier(major, minor, patch)`; the loader scans them with Reflections and picks the newest plugin `<=` the running version. The API started wrapping Minecraft (`BlockApi`, `PlayerApi`, `BlockStateApi`, `Vector3`, `ResourceIdentifier`).

### Lessons

* **The idea is right**: mod logic in a module without Minecraft types, small version/loader specific bridges, newest-compatible implementation selected at runtime.
* **It was mod-specific**: every mod would re-implement the bridge layer. Modularity turns that layer into a shared library.
* **Classpath scanning** (Reflections) is slow and fragile across loaders; loaders already have entrypoint systems (Fabric entrypoints, NeoForge `@Mod`).
* **Chunk-locator reflection** broke with every renderer change. Modern loaders offer *block entity render data* (Fabric `RenderDataBlockEntity`, NeoForge `ModelData`) that renderers (Sodium, Iris) snapshot properly, so the data should move back to a block entity hidden behind an API.
* **Mixins per mod** are the main source of incompatibility between mods. Shared, generic hook points (one mixin in the platform dispatching to many mods) are both safer and simpler.
* **Testing** never existed; game logic was only verifiable in-game.

## respro

Fluent builders for pack resources (`assets.setVariantBlockstate(id, variants -> ...)`, pack profile/meta/icon, recipes, KleeSlabs compat), registered as an always-enabled runtime pack via a `PackRepository` mixin, optional dump to disk in dev. Requirements it contributes: runtime resource/data packs, pack format per version, data generation.

## mythos-odyssey (`feature/base-for-magic-spells`)

`AbstractSpellProjectile` entity + `HowlingSpiritSpellProjectile`, GeckoLib `GeoModel`, Blockbench model, NeoForge config. Requirements: entity types and spawning, projectile behaviour, entity renderers/animated models, third-party library integration, config.

## Toolchain observations

* Builds use Loom (Fabric, Mojang mappings in `release/1.0`), NeoGradle (NeoForge). Java 17 (≤1.20.4) and 21 (≥1.20.5).
* Custom maven on Heroku (`prod-cebbys-repomanager.herokuapp.com`) is likely dead (Heroku free tier ended); artifacts should move to GitHub Packages or a static maven.
* GitHub Actions workflows use deprecated actions (`checkout@v2`, `upload-artifact@v2`).
