---
# modularity-arii
title: Port Fabric features to anvil
status: completed
type: epic
priority: normal
tags:
    - port
    - anvil
created_at: 2026-11-10T21:43:45Z
updated_at: 2026-11-11T23:47:02Z
---

Stage 3. `platforms/anvil` does not exist yet. This epic creates the shared Forge-family root, so NeoForge and Forge do not each copy the same mod-bus, registration and storage code.

Branch: `feature/port-anvil`.

Exit check: the root exists, the build assembles it for both Forge-family platforms, and each shared class differs from its NeoForge copy or the copy is removed. Gradle runs wait for the owner's word.

## Stage 3 status 2026-11-11
The anvil root exists with the stage order, the seam holder and the reflective detection; the NeoForge and Forge detection copies are gone. `javac` compiles the layer alone. Verification `yp34` needs a Gradle run.


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

## Tracker cleanup 2026-11-12

Closed as completed: every child bean is closed and the exit check ran green on 2026-11-12 (`-p modularity-gradle test`, the eight 21.1 smokes, docs/07 7.10). The root exists, both Forge-family platforms assemble it, and the NeoForge and Forge copies are gone.
