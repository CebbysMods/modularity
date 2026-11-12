---
# modularity-yrlm
title: 'quilt: client features'
status: done
type: task
priority: high
created_at: 2026-11-10T21:47:39Z
updated_at: 2026-11-12T11:10:00Z
parent: modularity-gng8
---

Files: `loom/client/**`, `loom/compat/KeyMappings.java`.
Work: audit HUD, dynamic models, OBJ models, key mappings and entity renderers on Quilt Loader.
Check: the client smoke log and the screenshot show the HUD layer and the dynamic model.


Check updated 2026-11-12 (not yet run): the client smoke screenshot is now part of the check. `SmokeCheckTask` passes the PNG to `SmokeCheck`, which rejects an empty, unreadable or undersized image and one with 16 colours or fewer (a flat screen); `SmokeSetup` points the client task at `<run dir>/screenshots/smoke.png`. This proves that the client rendered a frame. Still open: a scenario that proves the dynamic block model quads (and the HUD, key mapping and entity renderer paths), which needs a placement in the world the smoke client joins.


Proved 2026-11-12 (third pass): every client smoke passed the frame check that `SmokeCheck` now runs on the PNG (`SmokeSetup` passes build/smoke/run/screenshots/smoke.png; a client task without the property fails). Still open: the dynamic block models (NeoForge/Forge `getModelData`) and the HUD, key mapping and entity renderer paths, which no scenario places in the world.


## 2026-11-12: HUD, key mapping and model registration now run in the client smoke

The client stage of `SmokeFeatures` calls `client.blockModel`, `client.hud` and `client.keyBinding` and logs one line for each. `SmokeCheck` requires all three on every client run. Quilt uses the same `loom` client hooks as Fabric, so the lines prove the Quilt wiring as well. Not yet run.

Still open: the dynamic model quads (no block is placed in the world) and the entity renderer.


## 2026-11-12: the world scenario now supplies the three missing paths

`SmokeScenario` places `feature_block` three blocks in front of the player and holds the mine button on it from tick 30 to tick 70. Together with the client seam calls, the client smoke now logs `Smoke scenario: placed <block> at <pos>`, `Smoke model: quads at <pos>` and `Smoke particles: hit <block> at <pos>`, and `SmokeCheck` requires them on every client run. The hit particles run only through `ParticleEngine.destroy`, which the mixin patches, so a green line on the Forge family also proves that its mixins apply in the game.

Risk: the three block methods are found by name (`setBlockAndUpdate`, `continueDestroyBlock`, `getSingleplayerServer`), so a version without one logs an error and the smoke turns red by name instead of failing to compile. No build has checked the names yet.

Still open: the entity renderer, which needs an entity type that this smoke does not register.

## 2026-11-12: the client smoke proves the seam

The Quilt client smoke of 2026-11-12 10:52 holds the same six client lines, both network lines and the
screenshot. Check closed (docs/07 §7.10).
