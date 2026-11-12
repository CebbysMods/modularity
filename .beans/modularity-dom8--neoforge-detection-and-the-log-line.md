---
# modularity-dom8
title: 'neoforge: detection and the log line'
status: completed
type: task
priority: high
created_at: 2026-11-10T21:47:40Z
updated_at: 2026-11-11T23:46:53Z
parent: modularity-yrjc
---

Files: `neoforge/detect/{NeoForgePlatformSource,NeoForgeVersionSource}.java`.
Work: audit against the Fabric reference; the version comes from `FMLLoader`, the platform from the NeoForge marker class.
Check: the smoke log holds `platform neoforge:<version>, minecraft:<version>`.

## Audit 2026-11-11 (stage 4)
In the tree: the detection sources moved to `anvil` (`ModularityAnvilLoaderApi`, `ModularityAnvilPlatformSource`, `ModularityAnvilVersionSource`); the NeoForge copies are deleted. `NeoForgeLoaderInfo` stays in `neoforge` and reads the loaded API. `ModularityAnvilLifecycle.detect()` installs both sources and `ModularityAnvilLifecycle.common` writes the `[modularity] platform ...` line through `DetectionLog`. No Gradle run: AGENTS.md hard rule 1. Verification stays open in the verification bean.

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p modularity-gradle test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). The NeoForge log holds `(...) platform neoforge:21.1.265, minecraft:1.21.1`.
