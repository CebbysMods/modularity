---
# modularity-j2w5
title: 'forge: detection and the log line'
status: completed
type: task
priority: high
created_at: 2026-11-10T21:47:41Z
updated_at: 2026-11-11T23:38:49Z
parent: modularity-cuuj
---

Files: `anvil/detect/{ModularityAnvilPlatformSource,ModularityAnvilVersionSource}.java` (the Forge copies in
`forge/detect/` are deleted; `anvil` serves both Forge-family loaders by reflection).
Work: audit against the Fabric reference; Forge reports through `FMLLoader` and `Platform.isForge`.
Check: the smoke log holds `platform forge:<version>, minecraft:<version>`.

Done in the tree (no run yet). `ForgeLoaderInfo` returns the side from `FMLEnvironment.dist`, the loader version from `ModList.get().getModContainerById`, the development flag from `FMLEnvironment.production`, and `modularMods()` from `ModularModRegistry.entries()`. The Forge install point is the `ModularityForge` constructor and the old `forge/detect/` sources are removed, because `anvil/detect` serves both loaders by reflection. Check stays open: the smoke log must hold the `(side) platform forge:<version>, minecraft:<version>` line.

Status set 2026-11-12: the 2026-11-12 smoke run is green and the Forge logs hold `(side) platform forge:52.1.21, minecraft:1.21.1`; the smoke check requires that line. See docs/07 §7.10.
