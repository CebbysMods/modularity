---
# modularity-4r29
title: 'Phase 2: Fabric detection sources in platforms/loom'
status: todo
type: task
priority: normal
tags:
    - architecture
    - fabric
created_at: 2026-11-10T09:08:10Z
updated_at: 2026-11-10T09:08:10Z
parent: modularity-km3b
---

Add the Fabric Loader detection source to the shared loom layer and install it.

Tasks:
- [x] loom.detect.LoomVersionSource (ported from FabricVersionSource)
- [x] loom.detect.LoomPlatformSource (Fabric; reports QUILT through the QuiltLoader marker)
- [x] LoomEntrypoints.common installs both before ModularityCommon.init
- [x] platform.detect.DetectionLog: one [modularity] platform line
- [x] Do not switch MinecraftRuntime yet (see the plan, Ordering)

Verification (on request): -Pmodularity.platforms=fabric-21.1 buildAll and :21.1-fabric:smoke.

## Progress

2026-11-10: added `loom.detect.LoomClasses`, `LoomVersionSource` and `LoomPlatformSource`; `LoomEntrypoints.common(String)` installs both before the seams and calls `DetectionLog.log()` (new `platform.detect` class). `MinecraftRuntime` was not switched at this point, as the plan orders. Gradle tasks were not run (not requested).
