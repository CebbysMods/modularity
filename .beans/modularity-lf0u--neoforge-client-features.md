---
# modularity-lf0u
title: 'neoforge: client features'
status: done
type: task
priority: high
created_at: 2026-11-10T21:47:41Z
updated_at: 2026-11-12T11:10:00Z
parent: modularity-yrjc
---

Files: `neoforge/client/NeoForgeClientHooks.java`, `neoforge/loader/NeoForgeRenderData.java`.
Work: `blockModels` and `hud` warn, and `renderData` throws; key mappings, entity renderers, reload and tick are wired. Model baking, HUD layers and render data are the work.
Check: the client smoke log and the screenshot show the HUD layer and the dynamic model.

## Audit 2026-11-11 (stage 4)
In the tree: `NeoForgeClientHooks` registers key mappings, client payload listeners, entity renderers and the HUD (`RenderGuiEvent.Post`), and reload and tick listeners. `NeoForgeRenderData` returns the block entity of the position, for the game thread only.
## Open risk
Dynamic block models are not implemented: `blockModels` logs a warning. The loom `DynamicBakedModel` is a Fabric renderer and is not in the NeoForge stack; a NeoForge path needs its own baked-model code. No Gradle run: AGENTS.md hard rule 1. Verification stays open in the verification bean.

Correction, same day: dynamic block models are now implemented. `NeoForgeDynamicBakedModel` replaces the baked model of every state of the block in `ModelEvent.ModifyBakingResult`, resolves the emitted states in `getModelData` and returns their quads from `getQuads`. The item form renders nothing, as on Fabric. `javac` compiles it; no client run has rendered it. Remaining risk: the render path is unproven. Recorded in TODO-NOW stage 4.


Check updated 2026-11-12 (not yet run): the client smoke screenshot is now part of the check. `SmokeCheckTask` passes the PNG to `SmokeCheck`, which rejects an empty, unreadable or undersized image and one with 16 colours or fewer (a flat screen); `SmokeSetup` points the client task at `<run dir>/screenshots/smoke.png`. This proves that the client rendered a frame. Still open: a scenario that proves the dynamic block model quads (and the HUD, key mapping and entity renderer paths), which needs a placement in the world the smoke client joins.


Proved 2026-11-12 (third pass): every client smoke passed the frame check that `SmokeCheck` now runs on the PNG (`SmokeSetup` passes build/smoke/run/screenshots/smoke.png; a client task without the property fails). Still open: the dynamic block models (NeoForge/Forge `getModelData`) and the HUD, key mapping and entity renderer paths, which no scenario places in the world.


## 2026-11-12: HUD, key mapping and model registration now run in the client smoke

The client stage of `SmokeFeatures` calls `client.blockModel`, `client.hud` and `client.keyBinding` and logs one line for each (`Smoke model: registered for …`, `Smoke client: hud drawn <w>x<h>`, `Smoke keys: binding=… down=…`). `SmokeCheck` requires all three on every client run. The NeoForge paths behind them are `ModelEvent.ModifyBakingResult`, `RenderGuiEvent.Post` and `RegisterKeyMappingsEvent` (`NeoForgeClientHooks`). Not yet run; one authorized NeoForge client smoke closes the registration half.

Still open: the dynamic model quads (the block is never placed in the world) and the entity renderer, which needs an entity type this smoke does not register.


## 2026-11-12: the world scenario now supplies the three missing paths

`SmokeScenario` places `feature_block` three blocks in front of the player and holds the mine button on it from tick 30 to tick 70. Together with the client seam calls, the client smoke now logs `Smoke scenario: placed <block> at <pos>`, `Smoke model: quads at <pos>` and `Smoke particles: hit <block> at <pos>`, and `SmokeCheck` requires them on every client run. The hit particles run only through `ParticleEngine.destroy`, which the mixin patches, so a green line on the Forge family also proves that its mixins apply in the game.

Risk: the three block methods are found by name (`setBlockAndUpdate`, `continueDestroyBlock`, `getSingleplayerServer`), so a version without one logs an error and the smoke turns red by name instead of failing to compile. No build has checked the names yet.

Still open: the entity renderer, which needs an entity type that this smoke does not register.


## 2026-11-12: the client stage now runs inside the registry pass

The third smoke run of 2026-11-12 reached the entity renderer and crashed:
`NullPointerException … "entityrenderer" is null` at `EntityRenderDispatcher.shouldRender`. Cause, from the NeoForge
21.1.265 classes: `ClientHooks.initClientHooks` posts `EntityRenderersEvent.RegisterRenderers`,
`RegisterKeyMappingsEvent` and `RegisterClientReloadListenersEvent` while `Minecraft` is constructed, and
`CommonModLoader.load` posts `FMLCommonSetupEvent` and `FMLClientSetupEvent` after that, during the first resource
reload. The client stage on `FMLClientSetupEvent` therefore added every client listener too late.

Fix: `ModularityNeoForgeClient` only prepares the client hooks, and `ModularityAnvilClient.runPending` runs the client
stage at the end of the common stage, inside the first `RegisterEvent`; `NeoForgeCommon` calls it after `replay(event)`.
`ClientHooks.entityRenderer` and `ClientHooks.blockModels` now take an id and resolve the entity type or the block when
their own event fires, because the entity and block registries are not readable inside the registry pass;
`ClientRegistry` keys its tables by id for the same reason. Decision recorded in docs/07 §7.8.

Correction to the note above: the hit particles go through `ParticleEngine.crack`, not `ParticleEngine.destroy`.

Still open: no run has checked this. The next authorized run is the first NeoForge client run that can reach the entity
renderer, the mine scenario and the HUD at once.

## 2026-11-12: the client smoke proves the seam

The client stage runs at the end of the common stage inside the first `RegisterEvent` (docs/07 §7.8), so
`blockModels`, `entityRenderer`, `keyMapping` and `hud` all register a listener on the mod bus. The NeoForge
client smoke of 2026-11-12 10:52 holds `Smoke model: quads at …`, `Smoke client: hud drawn 427x240`,
`Smoke keys: binding=smoke_key down=false`, `Smoke particles: hit …`, `Smoke entity: rendered …`, both
network lines and the screenshot. Check closed (docs/07 §7.10).
