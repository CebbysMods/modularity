---
# modularity-h5vz
title: 'forge: verification with the smoke tests'
status: completed
type: task
priority: high
created_at: 2026-11-10T21:47:42Z
updated_at: 2026-11-11T23:46:53Z
parent: modularity-cuuj
---

Check: `-p build-logic test`; `-Pmodularity.platforms=forge-21.1 :platforms:21.1:smoke`; inspect `META-INF/jarjar/metadata.json`; `allureAggregateReport`.

Not run. Only a `javac --release 21` compile ran: 97 sources from platforms/minecraft/common, platforms/common/common, platforms/anvil/common and platforms/forge/common, exit 0, no errors. No Gradle task, no server smoke, no client smoke. Open checks, in order: `./gradlew -p build-logic test`; `./gradlew :platforms:21.1:21.1-forge:smokeServer`; the same for `smokeClient`; then the launch, platform, registration, payload, storage and HUD lines in the smoke output.



## Run 2026-11-11 (owner authorized the run)
- `./gradlew -p build-logic test`: BUILD SUCCESSFUL.
- `:platforms:21.1:21.1-forge:smokeServer`: `smoke SERVER 1.21.1: ok`, `build/smoke/server.txt` holds `Mod modularity: loaded=true version=0.1.0-SNAPSHOT nested=no`, `(server) platform forge:52.1.21, minecraft:1.21.1` and `Server ticked 40 times, stopping`.
- `:platforms:21.1:21.1-forge:smokeClient`: `smoke CLIENT 1.21.1: ok`, `(client) platform forge:52.1.21, minecraft:1.21.1`, `Client joined world`, `Screenshot: Saved screenshot as smoke.png`, `Client done`; the screenshot is at `build/smoke/run/screenshots/smoke.png`.
Still open: `META-INF/jarjar/metadata.json` inspection of the shipped jar, the jar-in-jar load in a real install (`nested=no` in the dev run), Forge mixins (not wired, PR-13) and `allureAggregateReport`.

## Re-run 2026-11-11 (strengthened check)
The final pass of the day runs `:platforms:21.1:21.1-forge:smokeServer` and `smokeClient` with the
strengthened `SmokeCheck`, which expects `Entrypoint mod-constructor active (forge)`, `launch`, `common`
and the side stage line. Both pass, so `build/smoke/{server,client}.txt` holds all of them. Still open:
the shipped jar and its jar-in-jar metadata, Forge mixins, `allureAggregateReport`.



Decision 2026-11-12: Forge mixins are a stated limit (PR-13, docs/07 7.8). The mixin part of this verification is therefore not a check to run: the Forge smoke must show no Modularity mixin and no mixin error. The remaining checks are the eight 21.1 smokes with the strengthened `SmokeCheck`, the dynamic-model frame (`modularity-hcru`), the shipped jar and `META-INF/jarjar/metadata.json`, and `allureAggregateReport`.


## Closed by the run of 2026-11-12

`./gradlew -p build-logic test` is green (50 tests). The eight 21.1 smokes pass for all four loaders, and
every server and client log holds both `Smoke features:` lines with `present=true`, so a block, an item and
a payload channel registered through the API resolve on both sides. The first pass failed on NeoForge and
Forge with `Registry is already frozen`: `NewRegistryEvent` precedes `GameData.unfreezeData`. The common
stage now runs on the first `RegisterEvent` and the entrypoint calls `replay(event)` on that loader's hook.
Recorded in docs/07 7.6, 7.8 and 7.10, TODO-NOW, PORT-REVIEW and bean `modularity-4uol`.



Correction 2026-11-12 (owner reversal): Forge mixins are required, not a stated limit. The Forge smoke now
has to show the config applying: the Forge jar carries MixinConfigs in its manifest and
modularity.forge.mixins.json is generated (docs/07 7.8, bean modularity-6rm0). A mixin error, or a log
with no mixin activity, fails the check.

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p build-logic test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). The Forge log holds no mixin error. A Forge development run cannot apply the config at all, so the mixin proof moved to `modularity-6rm0` and `modularity-xbj0`.
