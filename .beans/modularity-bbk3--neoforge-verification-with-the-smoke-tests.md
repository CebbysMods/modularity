---
# modularity-bbk3
title: 'neoforge: verification with the smoke tests'
status: completed
type: task
priority: high
created_at: 2026-11-10T21:47:41Z
updated_at: 2026-11-11T23:04:12Z
parent: modularity-yrjc
---

Check: `-p modularity-gradle test`; `-Pmodularity.platforms=neoforge-21.1 :platforms:21.1:smoke`; `allureAggregateReport`.

## Audit 2026-11-11 (stage 4)
The NeoForge code and the metadata changes are in the tree. The checks that close this bean are Gradle runs and need the owner's word: `-p modularity-gradle test`, `:platforms:21.1:21.1-neoforge:smokeServer` and `smokeClient`, and one real run that shows the NeoForge mixins apply. No Gradle run: AGENTS.md hard rule 1. Verification stays open in the verification bean.



## Run 2026-11-11 (owner authorized the run)
- `./gradlew -p modularity-gradle test`: BUILD SUCCESSFUL.
- `:platforms:21.1:21.1-neoforge:smokeServer`: `smoke SERVER 1.21.1: ok`, line `(server) platform neoforge:21.1.265, minecraft:1.21.1`.
- `:platforms:21.1:21.1-neoforge:smokeClient`: `smoke CLIENT 1.21.1: ok`, `Client joined world`, screenshot `smoke.png`, `Client done`.
One defect was found and fixed before these passed: ModDevGradle left the nested API off the run class path (fixed in `NeoForgePlatformSetup.addNestedModulesToRun`, docs/07 §7.8). This also caused the earlier mixin attach failure; the mixins and `Interceptors` are unchanged and the four loaders pass again with them.
Not proven: the shipped jar (`nested=no` in every dev run) and dynamic block models in a rendered frame. `allureAggregateReport` did not run.


## Closed by the run of 2026-11-12

`./gradlew -p modularity-gradle test` is green (50 tests). The eight 21.1 smokes pass for all four loaders, and
every server and client log holds both `Smoke features:` lines with `present=true`, so a block, an item and
a payload channel registered through the API resolve on both sides. The first pass failed on NeoForge and
Forge with `Registry is already frozen`: `NewRegistryEvent` precedes `GameData.unfreezeData`. The common
stage now runs on the first `RegisterEvent` and the entrypoint calls `replay(event)` on that loader's hook.
Recorded in docs/07 7.6, 7.8 and 7.10, TODO-NOW, PORT-REVIEW and bean `modularity-4uol`.
