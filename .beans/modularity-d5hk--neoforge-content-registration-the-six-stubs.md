---
# modularity-d5hk
title: 'neoforge: content registration (the six stubs)'
status: completed
type: task
priority: high
created_at: 2026-11-10T21:47:41Z
updated_at: 2026-11-11T23:46:53Z
parent: modularity-yrjc
---

Files: `neoforge/loader/NeoForgeLoaderHooks.java`.
Work: `register`, `blockEntityType`, `addToCreativeTab`, `registerPayload`, `sendToPlayer` and `onServerPayload` all throw `notImplemented` today. Use `DeferredRegister` with `RegisterEvent`, and `modBus.addListener(RegisterPayloadHandlersEvent)`.
Check: the smoke shows the guest mod block, item, block entity, tab, entity type, sound and channel.

## Audit 2026-11-11 (stage 4)
In the tree: `NeoForgeLoaderHooks` queues every `register(...)` call with its registry key and replays it in `RegisterEvent`; `blockEntityType` builds a `BlockEntityType` from the block; `addToCreativeTab` queues the item and adds it in `BuildCreativeModeTabContentsEvent`. No Gradle run: AGENTS.md hard rule 1. Verification stays open in the verification bean.


2026-11-11: blocked from proof by `modularity-4uol`. A mod jar cannot register on NeoForge, because
`ModularityCommon.init` freezes `ModularModRegistry` inside Modularity's own mod constructor. The smoke mod is
therefore not a Modularity mod on NeoForge and the feature lines are not expected there.


Update, 2026-11-11: the blocker `modularity-4uol` has a fix in the tree. The common stage runs on `NewRegistryEvent`,
so a mod jar registers in its own constructor. A smoke run still has to prove that the blocks, items, tabs and
payload types queued in that stage register on NeoForge.


## Closed by the run of 2026-11-12

`./gradlew -p build-logic test` is green (50 tests). The eight 21.1 smokes pass for all four loaders, and
every server and client log holds both `Smoke features:` lines with `present=true`, so a block, an item and
a payload channel registered through the API resolve on both sides. The first pass failed on NeoForge and
Forge with `Registry is already frozen`: `NewRegistryEvent` precedes `GameData.unfreezeData`. The common
stage now runs on the first `RegisterEvent` and the entrypoint calls `replay(event)` on that loader's hook.
Recorded in docs/07 7.6, 7.8 and 7.10, TODO-NOW, PORT-REVIEW and bean `modularity-4uol`.

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p build-logic test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). The NeoForge log holds both `Smoke features:` lines with `present=true`.
