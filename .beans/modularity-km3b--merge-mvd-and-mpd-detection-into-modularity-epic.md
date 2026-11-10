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
updated_at: 2026-11-10T10:49:14Z
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
