---
# modularity-4r29
title: 'Phase 2: Fabric detection sources in platforms/loom'
status: completed
type: task
priority: normal
tags:
    - architecture
    - fabric
created_at: 2026-11-10T09:08:10Z
updated_at: 2026-11-11T23:39:00Z
parent: modularity-km3b
---

Add the Fabric Loader detection source to the shared loom layer and install it.

Tasks:
- [x] loom.detect.ModularityLoomVersionSource (ported from ModularityFabricVersionSource)
- [x] loom.detect.ModularityLoomPlatformSource (Fabric; reports QUILT through the QuiltLoader marker)
- [x] LoomEntrypoints.common installs both before ModularityCommon.init
- [x] platform.detect.DetectionLog: one [modularity] platform line
- [x] Do not switch MinecraftRuntime yet (see the plan, Ordering)

Verification (on request): -Pmodularity.platforms=fabric-21.1 buildAll and :21.1-fabric:smoke.

## Progress

2026-11-10: added `loom.detect.ModularityLoomClasses`, `ModularityLoomVersionSource` and `ModularityLoomPlatformSource`; `LoomEntrypoints.common(String)` installs both before the seams and calls `DetectionLog.log()` (new `platform.detect` class). `MinecraftRuntime` was not switched at this point, as the plan orders. Gradle tasks were not run (not requested).

Status set 2026-11-12: `ModularityLoomClasses`, `ModularityLoomVersionSource` and `ModularityLoomPlatformSource` are the classes in the tree (the `Loom*` names are gone), installed by `ModularityLoomServices`. The 2026-11-12 run is green on Fabric and Quilt and the logs hold the platform line for both (docs/07 §7.10).
