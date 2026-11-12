---
# modularity-hcru
title: 'forge: client features'
status: done
type: task
priority: high
created_at: 2026-11-10T21:47:41Z
updated_at: 2026-11-12T11:10:00Z
parent: modularity-cuuj
---

Files: new `forge/client/**`.
Work: render data, dynamic models, HUD, key mappings, entity renderers, the OBJ reload listener and the client tick, through the Forge client events.
Check: the client smoke log and the screenshot show the HUD layer and the dynamic model.

Done in the tree (no run yet). `ForgeClientHooks` listens on the mod bus for `RegisterKeyMappingsEvent`, `EntityRenderersEvent.RegisterRenderers`, `RegisterClientReloadListenersEvent` and `AddGuiOverlayLayersEvent`, and on `MinecraftForge.EVENT_BUS` for `TickEvent.ClientTickEvent.Post`. HUD layers are added with `event.getLayeredDraw().addAbove(id, ForgeLayeredDraw.HOTBAR, ...)` and are skipped when `hideGui`, a screen or no player is present. Risk: Forge dynamic block models are not implemented; `blockModels` logs a warning, because the loom `DynamicBakedModel` is a Fabric renderer and is not in the Forge stack. The same limit is recorded for NeoForge. Check stays open: a client smoke must show the HUD layer, a key mapping and an entity renderer.

Correction, same day: dynamic block models are now implemented. `ForgeDynamicBakedModel` replaces the baked model of every state of the block in `ModelEvent.ModifyBakingResult`. Forge gives the model the position in `getModelData`, so the emitted states are computed there and `getQuads` returns the quads of the delegate models. The item form renders nothing, as on Fabric. `javac` compiles it; no client run has rendered it. Remaining risk: the render path is unproven.

## Event order checked 2026-11-11 (inspection, no run)
Forge 1.21.1 fires `RegisterKeyMappingsEvent`, `RegisterClientReloadListenersEvent` and
`EntityRenderersEvent.RegisterRenderers` from `ForgeHooksClient.initClientHooks`, which `Minecraft` calls in its own
constructor (line 587 of the decompiled source). `FMLClientSetupEvent` and its `enqueueWork` work run before the
`Minecraft` constructor, so the listeners `ForgeClientHooks` adds in the client stage are in time. Same order is
assumed for NeoForge and stays unproven until the client smoke runs.


Check updated 2026-11-12 (not yet run): the client smoke screenshot is now part of the check. `SmokeCheckTask` passes the PNG to `SmokeCheck`, which rejects an empty, unreadable or undersized image and one with 16 colours or fewer (a flat screen); `SmokeSetup` points the client task at `<run dir>/screenshots/smoke.png`. This proves that the client rendered a frame. Still open: a scenario that proves the dynamic block model quads (and the HUD, key mapping and entity renderer paths), which needs a placement in the world the smoke client joins.


Proved 2026-11-12 (third pass): every client smoke passed the frame check that `SmokeCheck` now runs on the PNG (`SmokeSetup` passes build/smoke/run/screenshots/smoke.png; a client task without the property fails). Still open: the dynamic block models (NeoForge/Forge `getModelData`) and the HUD, key mapping and entity renderer paths, which no scenario places in the world.


## 2026-11-12: HUD, key mapping and model registration now run in the client smoke

The client stage of `SmokeFeatures` calls `client.blockModel`, `client.hud` and `client.keyBinding` and logs one line for each. The Forge paths behind them are `ModelEvent.ModifyBakingResult`, `AddGuiOverlayLayersEvent` with `ForgeLayeredDraw.HOTBAR` and `RegisterKeyMappingsEvent` (`ForgeClientHooks`). `SmokeCheck` requires all three on every client run. Not yet run.

Still open: the dynamic model quads, the entity renderer and the hit particles, which only a mined block triggers.


## 2026-11-12: the world scenario now supplies the three missing paths

`SmokeScenario` places `feature_block` three blocks in front of the player and holds the mine button on it from tick 30 to tick 70. Together with the client seam calls, the client smoke now logs `Smoke scenario: placed <block> at <pos>`, `Smoke model: quads at <pos>` and `Smoke particles: hit <block> at <pos>`, and `SmokeCheck` requires them on every client run. The hit particles run only through `ParticleEngine.destroy`, which the mixin patches, so a green line on the Forge family also proves that its mixins apply in the game.

Risk: the three block methods are found by name (`setBlockAndUpdate`, `continueDestroyBlock`, `getSingleplayerServer`), so a version without one logs an error and the smoke turns red by name instead of failing to compile. No build has checked the names yet.

Still open: the entity renderer, which needs an entity type that this smoke does not register.


## 2026-11-12: the client stage now runs inside the registry pass

Forge posts the same client registration events as NeoForge while `Minecraft` is constructed
(`ClientHooks.initClientHooks`, including `AddGuiOverlayLayersEvent` through `gui.initModdedOverlays()`), and its
`FMLClientSetupEvent` comes later, during the first resource reload. The client stage therefore added the HUD layer,
the entity renderers and the key mappings after those events had fired.

Fix: `ModularityForgeClient.register` only prepares the client hooks, and `ModularityForge` runs
`ModularityAnvilClient.runPending("forge")` at the end of the common stage, inside the first `RegisterEvent`.
`ForgeClientHooks.entityRenderer` and `blockModels` now take an id and resolve the entity type or the block when their
event fires (`Wrap.entityType`, `Wrap.block`). Decision recorded in docs/07 §7.8.

Still open: no run has checked this. The Forge client smoke has not run at all yet.

## 2026-11-12: the fix, the run, and the stale note above

`blockModels` does not warn: it replaces the baked model of every state of each block in
`ModelEvent.ModifyBakingResult` (`ForgeDynamicBakedModel`), and it resolves the block by id inside the
event. The HUD layer is appended with `ForgeLayeredDraw.add(id, layer)`: `addAbove(…, HOTBAR, …)` fails on
Forge 52.1.21 with `Expected layer minecraft:hotbar was not found in stack minecraft:vanilla_root`.

Check, run 2026-11-12 10:52: the Forge client smoke holds the six client lines (`Smoke model: quads`,
`Smoke particles: hit`, `Smoke client: hud drawn 427x240`, `Smoke keys`, `Smoke entity: rendered`), both
network lines and the screenshot. Check closed (docs/07 §7.8, §7.10). Open note: one suppressed
`ServerboundPlayerActionPacket` error in the Forge client log (TODO-NOW.md).
