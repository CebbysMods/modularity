---
# modularity-gng8
title: Port Fabric features to quilt
status: in-progress
type: epic
priority: normal
tags:
    - port
    - quilt
created_at: 2026-11-10T21:43:45Z
updated_at: 2026-11-11T23:04:08Z
---

Stage 2. `platforms/quilt` owns four entrypoints and takes the rest from `loom`. The gap is guest mods: `ModularityQuiltLaunch` reports them and does not load them (docs/08), and the separation work is bean modularity-nwv9.

Branch: `feature/port-quilt`.

Exit check: every functionality area of the loom epic exists on Quilt, the Quilt smoke of 21.1 passes, and docs/08 states which guest features Quilt supports. Gradle runs wait for the owner's word.


## Closed by the run of 2026-11-12

`./gradlew -p build-logic test` is green (50 tests). The eight 21.1 smokes pass for all four loaders, and
every server and client log holds both `Smoke features:` lines with `present=true`, so a block, an item and
a payload channel registered through the API resolve on both sides. The first pass failed on NeoForge and
Forge with `Registry is already frozen`: `NewRegistryEvent` precedes `GameData.unfreezeData`. The common
stage now runs on the first `RegisterEvent` and the entrypoint calls `replay(event)` on that loader's hook.
Recorded in docs/07 7.6, 7.8 and 7.10, TODO-NOW, PORT-REVIEW and bean `modularity-4uol`.

## Delta against Fabric, 2026-11-12

Quilt reaches 91 against Fabric 95. The delta is guest mods only (docs/08: Quilt reports a guest and does
not load it), plus the two open checks that touch every loader: a rendered dynamic-model frame and a load
from the shipped jar. PORT-REVIEW carries the table.
