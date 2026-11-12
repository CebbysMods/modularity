---
# modularity-l2mx
title: 'loom: verification with the smoke tests'
status: completed
type: task
priority: high
created_at: 2026-11-10T21:44:13Z
updated_at: 2026-11-11T23:04:12Z
parent: modularity-q4hh
---

Check: `-p modularity-gradle test`; `-Pmodularity.platforms=fabric-21.1 :platforms:21.1:smoke`; the same for `quilt-21.1`; then `allureAggregateReport`.
Record the run in docs/07 §7.10 and in the epic body. A check that did not run stays open.



2026-11-10 (branch feature/port-loom): stage 1 audit. The work is in the tree. `grep` finds no `UnsupportedOperationException` and no `notImplemented` under `platforms/loom`. The Fabric root now holds only `fabric/entrypoint` and `fabric/guest`, so each functionality lives in one place. `javac` compiled the loom, fabric and quilt sources against the 1.21.1 mapped jar and the Fabric, Quilt and Allure class paths with no error. No Gradle run (hard rule 1), so every smoke check below stays open: `-p modularity-gradle test`, `-Pmodularity.platforms=fabric-21.1 :platforms:21.1:smoke`, `-Pmodularity.platforms=quilt-21.1 :platforms:21.1:smoke`.
Specific: this bean is the check list above; it stays open until a Gradle run.



## Run 2026-11-11 (owner authorized the run)
- `./gradlew -p modularity-gradle test`: BUILD SUCCESSFUL.
- `:platforms:21.1:21.1-fabric:smokeServer` and `smokeClient`: both ok; `(server) platform fabric:0.20.1, minecraft:1.21.1` and `(client) platform fabric:0.20.1, minecraft:1.21.1`, world join and screenshot on the client.
- `:platforms:21.1:21.1-quilt:smokeServer` and `smokeClient`: both ok; `(server) platform quilt:0.30.2, minecraft:1.21.1` and `(client) platform quilt:0.30.2, minecraft:1.21.1`, world join and screenshot on the client.
The run used `smokeServer` and `smokeClient`, not the aggregate `smoke` task, and the daemon was not stopped in between. `allureAggregateReport` did not run. The shipped jar stays unproven: every dev run reports `nested=no`. Recorded in docs/07 §7.10.

## Re-run 2026-11-11 (strengthened check, shared layer unchanged)
The final pass of the day runs `:platforms:21.1:21.1-fabric:smokeServer` and `smokeClient` with the
strengthened `SmokeCheck` (entrypoint stage lines) and `platforms/minecraft` byte-identical to `HEAD`.
Both pass. `-p modularity-gradle test` ran earlier in the same pass and passed.


Verification, 2026-11-11 (feature smoke): `SmokeCheck` now also expects `Smoke features: declared block=… item=…
channel=…` and `Smoke features: resolved server block=… present=true` on Fabric (`SmokeCheckTest` covers both, and a
missing line fails the check). The smoke mod is a Modularity mod through its `"modularity"` entrypoint
(`modularity-smoke/minecraft/.../SmokeFeatures.java`), so a Fabric smoke run now proves content registration, not
only the lifecycle. No Gradle run has done so yet.


## Closed by the run of 2026-11-12

`./gradlew -p modularity-gradle test` is green (50 tests). The eight 21.1 smokes pass for all four loaders, and
every server and client log holds both `Smoke features:` lines with `present=true`, so a block, an item and
a payload channel registered through the API resolve on both sides. The first pass failed on NeoForge and
Forge with `Registry is already frozen`: `NewRegistryEvent` precedes `GameData.unfreezeData`. The common
stage now runs on the first `RegisterEvent` and the entrypoint calls `replay(event)` on that loader's hook.
Recorded in docs/07 7.6, 7.8 and 7.10, TODO-NOW, PORT-REVIEW and bean `modularity-4uol`.
