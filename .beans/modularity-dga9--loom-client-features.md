---
# modularity-dga9
title: 'loom: client features'
status: done
type: task
priority: high
created_at: 2026-11-10T21:44:12Z
updated_at: 2026-11-12T11:10:00Z
parent: modularity-q4hh
---

Files: `loom/client/{ModularityLoomClientHooks,HudHook,DynamicBakedModel,DynamicUnbakedModel,ObjModelReloadListener}.java`, `loom/compat/KeyMappings.java`.
Work: audit render data, dynamic block models, HUD, key mappings, entity renderers, OBJ models and hit particles.
Check: the client smoke log and screenshot show the HUD layer and the dynamic model.

Audit 2026-11-10: `ModularityLoomClientHooks` covers key mappings, payload receive and send, entity renderers, dynamic block models through Fabric model loading, HUD, resource reload and client tick; the models and the HUD hook are in `loom/client`. No stub.



2026-11-10 (branch feature/port-loom): stage 1 audit. The work is in the tree. `grep` finds no `UnsupportedOperationException` and no `notImplemented` under `platforms/loom`. The Fabric root now holds only `fabric/entrypoint` and `fabric/guest`, so each functionality lives in one place. `javac` compiled the loom, fabric and quilt sources against the 1.21.1 mapped jar and the Fabric, Quilt and Allure class paths with no error. No Gradle run (hard rule 1), so every smoke check below stays open: `-p modularity-gradle test`, `-Pmodularity.platforms=fabric-21.1 :platforms:21.1:smoke`, `-Pmodularity.platforms=quilt-21.1 :platforms:21.1:smoke`.
Specific: `ModularityLoomClientHooks` covers key mappings, entity renderers, model baking, HUD, reload and tick; `ModularityLoomRenderData` serves the render data.


Check updated 2026-11-12 (not yet run): the client smoke screenshot is now part of the check. `SmokeCheckTask` passes the PNG to `SmokeCheck`, which rejects an empty, unreadable or undersized image and one with 16 colours or fewer (a flat screen); `SmokeSetup` points the client task at `<run dir>/screenshots/smoke.png`. This proves that the client rendered a frame. Still open: a scenario that proves the dynamic block model quads (and the HUD, key mapping and entity renderer paths), which needs a placement in the world the smoke client joins.


Proved 2026-11-12 (third pass): every client smoke passed the frame check that `SmokeCheck` now runs on the PNG (`SmokeSetup` passes build/smoke/run/screenshots/smoke.png; a client task without the property fails). Still open: the dynamic block models (NeoForge/Forge `getModelData`) and the HUD, key mapping and entity renderer paths, which no scenario places in the world.


## 2026-11-12: HUD, key mapping and model registration now run in the client smoke

The client stage of `SmokeFeatures` now calls `client.blockModel`, `client.hud` and `client.keyBinding`, and logs one line for each: `Smoke model: registered for <block>`, `Smoke client: hud drawn <w>x<h>` and `Smoke keys: binding=<name> down=<true|false>`. `SmokeCheck` expects all three on every client run, and `SmokeCheckTest.clientNeedsTheClientSeam` fails when one is missing. Not yet run: the lines close this bean only after one authorized client smoke on Fabric and Quilt.

Still open: the quads of the dynamic model. No smoke places the block in the joined world, so `emitBlockQuads` has not run. The scenario needs a world write, and the portable `Level` API has none; the next step is a placement through the vanilla `Level` of each smoke root, or a small placement method in the API.


## 2026-11-12: the world scenario now supplies the three missing paths

`SmokeScenario` places `feature_block` three blocks in front of the player and holds the mine button on it from tick 30 to tick 70. Together with the client seam calls, the client smoke now logs `Smoke scenario: placed <block> at <pos>`, `Smoke model: quads at <pos>` and `Smoke particles: hit <block> at <pos>`, and `SmokeCheck` requires them on every client run. The hit particles run only through `ParticleEngine.destroy`, which the mixin patches, so a green line on the Forge family also proves that its mixins apply in the game.

Risk: the three block methods are found by name (`setBlockAndUpdate`, `continueDestroyBlock`, `getSingleplayerServer`), so a version without one logs an error and the smoke turns red by name instead of failing to compile. No build has checked the names yet.

Still open: the entity renderer, which needs an entity type that this smoke does not register.

## 2026-11-12: the client smoke proves the seam

The Fabric client smoke of 2026-11-12 10:52 holds `Smoke model: quads at …`, `Smoke particles: hit …`,
`Smoke client: hud drawn 427x240`, `Smoke keys: binding=smoke_key down=false`, `Smoke entity: rendered …`,
both network lines and the screenshot. Check closed (docs/07 §7.10).
