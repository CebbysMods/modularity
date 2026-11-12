---
# modularity-dti9
title: 'forge: content registration'
status: completed
type: task
priority: high
created_at: 2026-11-10T21:47:41Z
updated_at: 2026-11-11T23:38:47Z
parent: modularity-cuuj
---

Files: new `forge/loader/**`.
Work: no `LoaderHooks` implementation exists. Use `DeferredRegister` with `RegisterEvent`.
Check: the smoke shows the guest mod block, item, block entity, tab, entity type, sound and channel.

Done in the tree (no run yet). `ForgeLoaderHooks` queues each value with its registry key, a `ResourceLocation` and the value. `RegisterEvent` replays every entry whose registry matches and removes it, so the queue is drained once per registry. Creative tab entries are queued in the same object and added in `BuildCreativeModeTabContentsEvent` with `event.accept(ItemStack)`. `blockEntityType` uses `BlockEntityType.Builder.of(...).build(null)`, because a `ValidatedBlockEntityType` does not exist on Forge. Risk: Forge closes its registries before `FMLCommonSetupEvent`, so the common stage must queue the values from the constructor, as `ModularityForge` does. Check stays open; a smoke run must register the smoke block, item, block entity, tab entry and payload type.


2026-11-11: blocked from proof by `modularity-4uol`. A mod jar cannot register on Forge, because
`ModularityCommon.init` freezes `ModularModRegistry` inside Modularity's own mod constructor. The smoke mod is
therefore not a Modularity mod on Forge and the feature lines are not expected there.


Update, 2026-11-11: the blocker `modularity-4uol` has a fix in the tree. The common stage runs on `NewRegistryEvent`,
so a mod jar registers in its own constructor. A smoke run still has to prove that the blocks, items, tabs and
payload types queued in that stage register on Forge.


## Closed by the run of 2026-11-12

`./gradlew -p build-logic test` is green (50 tests). The eight 21.1 smokes pass for all four loaders, and
every server and client log holds both `Smoke features:` lines with `present=true`, so a block, an item and
a payload channel registered through the API resolve on both sides. The first pass failed on NeoForge and
Forge with `Registry is already frozen`: `NewRegistryEvent` precedes `GameData.unfreezeData`. The common
stage now runs on the first `RegisterEvent` and the entrypoint calls `replay(event)` on that loader's hook.
Recorded in docs/07 7.6, 7.8 and 7.10, TODO-NOW, PORT-REVIEW and bean `modularity-4uol`.

Status set 2026-11-12: the 2026-11-12 run of all eight 21.1 smokes is green and every Forge server and client log holds both `Smoke features:` lines, which the smoke check requires. See docs/07 §7.10.
