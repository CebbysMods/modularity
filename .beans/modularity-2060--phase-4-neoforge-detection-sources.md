---
# modularity-2060
title: 'Phase 4: NeoForge detection sources'
status: completed
type: task
priority: normal
tags:
    - architecture
    - neoforge
created_at: 2026-11-10T09:08:10Z
updated_at: 2026-11-11T23:39:00Z
parent: modularity-km3b
---

Add the NeoForge detection source next to the installed seams.

Tasks:
- [x] neoforge.detect.NeoForgeVersionSource (FMLLoader.versionInfo().mcVersion())
- [x] neoforge.detect.NeoForgePlatformSource (neoForgeVersion, FMLEnvironment.dist, isProduction)
- [x] ModularityNeoForgeCommon installs both in the constructor and logs the detection
- [x] Keep all loader calls lazy (constructor runs before common setup)

Verification (on request): -Pmodularity.platforms=neoforge-21.1 buildAll and :21.1-neoforge:smoke.

## Progress

2026-11-10: added `neoforge.detect.NeoForgeVersionSource` and `NeoForgePlatformSource`, both using
`FMLLoader` calls inside `detect()` only. `ModularityNeoForgeCommon` installs them in its constructor
next to `Loaders.install(...)`; `DetectionLog.log()` runs in `enqueueWork`, after the parallel setup.
Gradle tasks were not run (not requested).

Status set 2026-11-12: the NeoForge detection sources now live in `anvil/detect` (`ModularityAnvilLoaderApi`, `ModularityAnvilPlatformSource`, `ModularityAnvilVersionSource`) and the `neoforge/detect` copies are deleted. The 2026-11-12 run is green on NeoForge and the logs hold `(server) platform neoforge:21.1.265, minecraft:1.21.1` and the client line (docs/07 §7.10).
