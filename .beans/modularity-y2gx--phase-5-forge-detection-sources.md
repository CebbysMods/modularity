---
# modularity-y2gx
title: 'Phase 5: Forge detection sources'
status: completed
type: task
priority: normal
tags:
    - architecture
    - forge
created_at: 2026-11-10T09:08:11Z
updated_at: 2026-11-11T23:39:00Z
parent: modularity-km3b
---

Add the Forge detection source; the full Forge runtime stays in modularity-g420.

Tasks:
- [x] forge.detect.ModularityForgeVersionSource (FMLLoader.versionInfo().mcVersion())
- [x] forge.detect.ModularityForgePlatformSource (forgeVersion, FMLEnvironment.dist, isProduction)
- [x] ModularityForge installs both in the constructor and logs the detection
- [x] Replace the comment that says the platform loads with mvd and mpd as separate mods

Verification (on request): -Pmodularity.platforms=forge-21.1 buildAll and :21.1-forge:smoke.

## Progress

2026-11-10: added `forge.detect.ModularityForgeVersionSource` and `ModularityForgePlatformSource`;
`ModularityForge` installs both in its constructor and calls `DetectionLog.log()` there. The full
Forge runtime stays in `modularity-g420`. Gradle tasks were not run (not requested).

Status set 2026-11-12: the Forge detection sources now live in `anvil/detect` and the `forge/detect` copies are deleted; `ModularityForge` installs them in its constructor. The 2026-11-12 run is green on Forge and the logs hold `(server) platform forge:52.1.21, minecraft:1.21.1` and the client line (docs/07 §7.10).
