---
# modularity-yrjc
title: Port Fabric features to neoforge
status: in-progress
type: epic
priority: normal
tags:
    - port
    - neoforge
created_at: 2026-11-10T21:43:46Z
updated_at: 2026-11-11T23:04:08Z
---

Stage 4. `platforms/neoforge` has detection, the seams and three entrypoints. It has no launch stage, no registration compat (creative tabs, payloads, block views, storages, key mappings), no HUD or dynamic models, and no guest mods.

Branch: `feature/port-neoforge`.

Exit check: every functionality area of the loom epic exists on NeoForge, and the 21.1 NeoForge smoke passes. Gradle runs wait for the owner's word.

## Stage 4 status 2026-11-11
The NeoForge areas are in the tree and `javac`-clean: lifecycle `0m6q`, detection `dom8`, registration `d5hk`, networking `ra61`, storage `gz97`, client `lf0u`, mixins `9rzu`; guests `jf4i` are scrapped with a recorded decision. Verification `bbk3` and the smoke checks need a Gradle run. Open risk: dynamic block models are not implemented on NeoForge.


New blocker, 2026-11-11: `modularity-4uol`. A dependent mod cannot register on this loader family, because
`ModularityCommon.init` freezes `ModularModRegistry` inside Modularity's own mod constructor. The feature smoke and
the content-registration, networking, storage and client proof wait for that decision.


Correction, 2026-11-11: `modularity-4uol` has a loader-side fix in the tree. The Forge-family common stage runs on
`NewRegistryEvent`, after every mod constructor, so a dependent mod registers in its own constructor. The feature
smoke covers this family too. No run has proved it yet.


## Closed by the run of 2026-11-12

`./gradlew -p modularity-gradle test` is green (50 tests). The eight 21.1 smokes pass for all four loaders, and
every server and client log holds both `Smoke features:` lines with `present=true`, so a block, an item and
a payload channel registered through the API resolve on both sides. The first pass failed on NeoForge and
Forge with `Registry is already frozen`: `NewRegistryEvent` precedes `GameData.unfreezeData`. The common
stage now runs on the first `RegisterEvent` and the entrypoint calls `replay(event)` on that loader's hook.
Recorded in docs/07 7.6, 7.8 and 7.10, TODO-NOW, PORT-REVIEW and bean `modularity-4uol`.

## Delta against Fabric, 2026-11-12

NeoForge reaches 90 against Fabric 95. The delta is guest mods (no public route) plus the two open checks
that touch every loader: a rendered dynamic-model frame and a load from the shipped jar. NeoForge applies
its mixins, so the mixin area is at parity. PORT-REVIEW carries the table.
