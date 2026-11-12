---
# modularity-k9eo
title: 'anvil: detection and the log line'
status: completed
type: task
priority: high
created_at: 2026-11-10T21:47:40Z
updated_at: 2026-11-11T23:46:53Z
parent: modularity-arii
---

Files: new `anvil/detect/**`.
Work: one shared source that reads the loader name from the loader API in use; each loader root keeps only what differs.
Check: each smoke log holds its own `platform <loader>:<version>, minecraft:<version>` line.

## Audit 2026-11-11 (stage 3)
In the tree: `ModularityAnvilLoaderApi` reads `FMLLoader`, `FMLEnvironment` and the version-info object by reflection for both families (`neoForgeVersion`/`neoforge-fml` and `forgeVersion`/`forge-fml`), so `anvil` names no loader type. `ModularityAnvilPlatformSource` reports loader, version, side and development; `ModularityAnvilVersionSource` reports the Minecraft version. The NeoForge and Forge detection copies are deleted. No Gradle run: AGENTS.md hard rule 1. Verification stays open.

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p build-logic test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). The NeoForge and Forge logs each hold their own detection line.
