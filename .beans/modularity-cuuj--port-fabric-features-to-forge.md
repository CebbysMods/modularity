---
# modularity-cuuj
title: Port Fabric features to forge
status: in-progress
type: epic
priority: normal
tags:
    - port
    - forge
created_at: 2026-11-10T21:43:46Z
updated_at: 2026-11-11T23:34:32Z
---

Stage 5. `platforms/forge` holds the two detection sources and one entrypoint that only logs. Every other feature is missing.

Branch: `feature/port-forge`.

Exit check: every functionality area of the loom epic exists on Forge, the 21.1 Forge smoke passes, and the universal jar carries the Forge metadata. Gradle runs wait for the owner's word.

Stage 5 state, 2026-11-11: the Forge sources are in the tree and `javac --release 21` compiled all 97 sources of the anvil and forge stacks with exit 0. No Gradle task ran, so no smoke proves the stage. Open: mixins (`modularity-6rm0`, blocked by the toolchain), dynamic block models (`modularity-hcru`), and every smoke check (`modularity-h5vz`).



Stage 5 state, 2026-11-11 (run): `./gradlew -p modularity-gradle test` passes, and the Forge server and client smokes pass for `:platforms:21.1:21.1-forge` (`(server)/(client) platform forge:52.1.21, minecraft:1.21.1`, world join and screenshot on the client). The lifecycle, detection, registration, networking, storage and client seams are therefore proven in a development run. Still open: Forge mixins (`modularity-6rm0`, no declaration route yet), dynamic block models in a rendered frame (`modularity-hcru`), the shipped jar and its jar-in-jar load (`nested=no` in every dev run), and `allureAggregateReport`.


New blocker, 2026-11-11: `modularity-4uol`. A dependent mod cannot register on this loader family, because
`ModularityCommon.init` freezes `ModularModRegistry` inside Modularity's own mod constructor. The feature smoke and
the content-registration, networking, storage and client proof wait for that decision.


Correction, 2026-11-11: `modularity-4uol` has a loader-side fix in the tree. The Forge-family common stage runs on
`NewRegistryEvent`, after every mod constructor, so a dependent mod registers in its own constructor. The feature
smoke covers this family too. No run has proved it yet.



Decision 2026-11-12: Forge mixins are a stated limit, not an open task. PR-13 now records it (docs/00), with the evidence in docs/07 7.8 and the PORT-REVIEW row. `modularity-6rm0` is closed; the optional Forge-only artifact is `modularity-7wk5`. This closes the mixin area of this epic in the same sense as Fabric: the loader decides what applies.


## Closed by the run of 2026-11-12

`./gradlew -p modularity-gradle test` is green (50 tests). The eight 21.1 smokes pass for all four loaders, and
every server and client log holds both `Smoke features:` lines with `present=true`, so a block, an item and
a payload channel registered through the API resolve on both sides. The first pass failed on NeoForge and
Forge with `Registry is already frozen`: `NewRegistryEvent` precedes `GameData.unfreezeData`. The common
stage now runs on the first `RegisterEvent` and the entrypoint calls `replay(event)` on that loader's hook.
Recorded in docs/07 7.6, 7.8 and 7.10, TODO-NOW, PORT-REVIEW and bean `modularity-4uol`.

## Delta against Fabric, 2026-11-12

This epic is the lowest-scoring platform (81 against Fabric 95). The delta is four rows and none of them is
unassigned: Forge mixins (stated limit above), guest mods (no public route), a rendered dynamic-model frame
(open check), and a load from the shipped jar (open check). PORT-REVIEW now carries the same table. The
epic stays `in-progress` until the owner either accepts the remaining rows as limits or names the next
route; the mixin row is already accepted.



Correction 2026-11-12 (owner reversal): mixins are required on every platform, and Forge is now wired,
not limited. The Forge jar names its config in the manifest (MixinConfigs); the config is generated like
NeoForge's (docs/07 7.8, bean modularity-6rm0). The row in the parity table above that calls Forge mixins
a stated limit is obsolete; the delta that remains is the smoke proof and the universal jar
(modularity-xbj0).
