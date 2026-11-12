---
# modularity-km3b
title: Merge mvd and mpd detection into Modularity (epic)
status: in-progress
type: epic
priority: normal
tags:
    - architecture
    - build
created_at: 2026-11-10T09:07:59Z
updated_at: 2026-11-11T23:39:06Z
---

Implements .beans/modularity-9oz9 (merge mvd/mpd, retire companion-mods). Plan: C:\Users\valen\.claude-ollama\plans\serene-puzzling-otter.md

Phases (child beans):
1. mvd/mpd to api.detect
2. Fabric (shared loom sources)
3. Quilt
4. NeoForge
5. Forge
6. Switch MinecraftRuntime, remove utilities, docs

Acceptance:
- Modularity detects version, loader and side in development and from the universal jar on Fabric, Quilt, NeoForge and Forge, with no separate detector mod installed.
- utilities/mvd and utilities/mpd are deleted; companion-mods no longer appears in the table, code or metadata.
- Smoke asserts Modularity's own detection log line on all four loaders.
- Docs describe one mod, not three.

Risks are tracked in TODO-NOW.md.

## Progress

2026-11-10: all six phases are implemented in the working tree. `api.detect` and the sealed `Version`
types are in `modularity-api`; `loom`, `neoforge` and `forge` each install their own sources; the
`[modularity] platform ...` line replaces `[mvd]` and `[mpd]`; `utilities/` is deleted. The acceptance
criteria stay unchecked: no Gradle task ran (not requested), so no loader has reported the new line
in a smoke run yet. The eight checks in TODO-NOW.md stay open.


2026-11-10 (after the six static checks): the Quilt loader-version check passed.
`:platforms:21.1:21.1-quilt:smokeServer` logged `[modularity] platform QUILT 0.30.2, Minecraft
1.21.1, SERVER`; the version matches `quilt-loader` in `gradle/platforms.toml`. Quilt is the first
loader to report the new line after the merge. The run is recorded in docs/07 §7.10.

One open check remains: no `META-INF/services` entry in the universal jar (`modularity-f5e2`).
Note: `platforms/quilt/versions/21.1/build/libs` still held a 2026-11-09 platform jar that names `mvd`
and `mpd` in its metadata. That jar predates the merge and must not be used as evidence.


2026-11-10 (owner request): the detection line template changed to
`(<side>) platform <loader>:<loaderVersion>, minecraft:<version>`. The Quilt run above used
the earlier format. `DetectionLog` writes the new line; `SmokeCheck` and `SmokeCheckTest` expect it;
docs 00, 06 and 07 and the two review documents quote it. The new line is not yet seen in a smoke run.


2026-11-10 (owner request, same change): the other Modularity log lines lost the `[modularity]` prefix
too. `EntryPoints` writes `entrypoint <role> active (<loader>)`; the Fabric launch writes
`Loaded guest mod ...`; NeoForge and Forge write `NeoForge platform loaded` and `Forge platform
loaded`; Quilt's error line starts with `Guest mods are not supported`. `SmokeCheck` expects the guest
line without the prefix.

2026-11-10 (three runs, owner-approved): all eight merge checks pass, so TODO-NOW.md no longer lists open items for the merge.

- `./gradlew -p build-logic test`: BUILD SUCCESSFUL, 46 tests, no failure. The seven `SmokeCheck` tests expect the capital-letter line format.
- `./gradlew -Pmodularity.platforms=21.1 :platforms:21.1:jar`: BUILD SUCCESSFUL in 9s. The universal jar holds 495 files and no `META-INF/services` entry (`modularity-f5e2` closed).
- `./gradlew -Pmodularity.platforms=quilt-21.1 :platforms:21.1:21.1-quilt:smokeServer`: `smoke SERVER 1.21.1: ok`. The log shows `(server) platform quilt:0.30.2, minecraft:1.21.1`, `Mod modularity: loaded=true version=0.1.0-SNAPSHOT nested=no` and `Server ticked 40 times, stopping`, plus the three Quilt entrypoints.

Acceptance still open: the detection line must be seen on Fabric, NeoForge and Forge. Quilt is the only loader with a run. The S5 `smokeAll` pass in TODO-NOW.md covers the rest.

2026-11-12: the detection line is now seen on all four loaders. The 2026-11-12 run is green and docs/07 §7.10 records `(server)/(client) platform fabric:0.20.1|quilt:0.30.2|neoforge:21.1.265|forge:52.1.21, minecraft:1.21.1`. Phases 2 to 5 are closed with that evidence. The epic stays open for one acceptance bullet: detection from the universal jar, which no run has shown (bean `modularity-xbj0`).
