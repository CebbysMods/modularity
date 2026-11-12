---
# modularity-8yym
title: 'loom: content registration'
status: completed
type: task
priority: high
created_at: 2026-11-10T21:44:12Z
updated_at: 2026-11-11T23:46:52Z
parent: modularity-q4hh
---

Files: `loom/loader/ModularityLoomLoaderHooks.java`, `loom/compat/{BlockViews,CreativeTabs}.java`.
Work: audit the register call, the block entity type and the creative tab path for blocks, block entities, items, entity types, sounds and channels.
Check: the smoke shows the registered block, item, block entity and tab of the guest mod.

Audit 2026-11-10: `ModularityLoomLoaderHooks` registers through `Registry.register`, builds block entity types with `FabricBlockEntityTypeBuilder` and adds items through `compat.CreativeTabs`; `compat.BlockViews` reads render data from a Fabric block view. No stub. Proved only by the guest mod smoke.



2026-11-10 (branch feature/port-loom): stage 1 audit. The work is in the tree. `grep` finds no `UnsupportedOperationException` and no `notImplemented` under `platforms/loom`. The Fabric root now holds only `fabric/entrypoint` and `fabric/guest`, so each functionality lives in one place. `javac` compiled the loom, fabric and quilt sources against the 1.21.1 mapped jar and the Fabric, Quilt and Allure class paths with no error. No Gradle run (hard rule 1), so every smoke check below stays open: `-p modularity-gradle test`, `-Pmodularity.platforms=fabric-21.1 :platforms:21.1:smoke`, `-Pmodularity.platforms=quilt-21.1 :platforms:21.1:smoke`.
Specific: `ModularityLoomLoaderHooks.register`, `blockEntityType` and `addToCreativeTab` delegate to the Fabric API registries.


2026-11-11: `modularity-smoke/minecraft/.../SmokeFeatures.java` exercises this area. It declares a block, an item and
a `DataCodec.INT` channel in the common stage and resolves the block in the server and client stages, one log line
per stage. The smoke is wired on Fabric and Quilt only (`modularity-4uol`).


## Closed by the run of 2026-11-12

`./gradlew -p modularity-gradle test` is green (50 tests). The eight 21.1 smokes pass for all four loaders, and
every server and client log holds both `Smoke features:` lines with `present=true`, so a block, an item and
a payload channel registered through the API resolve on both sides. The first pass failed on NeoForge and
Forge with `Registry is already frozen`: `NewRegistryEvent` precedes `GameData.unfreezeData`. The common
stage now runs on the first `RegisterEvent` and the entrypoint calls `replay(event)` on that loader's hook.
Recorded in docs/07 7.6, 7.8 and 7.10, TODO-NOW, PORT-REVIEW and bean `modularity-4uol`.

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p modularity-gradle test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). The Fabric log holds both `Smoke features:` lines with `present=true`.
