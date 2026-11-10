---
# modularity-y2gx
title: 'Phase 5: Forge detection sources'
status: todo
type: task
priority: normal
tags:
    - architecture
    - forge
created_at: 2026-11-10T09:08:11Z
updated_at: 2026-11-10T09:08:11Z
parent: modularity-km3b
---

Add the Forge detection source; the full Forge runtime stays in modularity-g420.

Tasks:
- [x] forge.detect.ForgeVersionSource (FMLLoader.versionInfo().mcVersion())
- [x] forge.detect.ForgePlatformSource (forgeVersion, FMLEnvironment.dist, isProduction)
- [x] ModularityForge installs both in the constructor and logs the detection
- [x] Replace the comment that says the platform loads with mvd and mpd as separate mods

Verification (on request): -Pmodularity.platforms=forge-21.1 buildAll and :21.1-forge:smoke.

## Progress

2026-11-10: added `forge.detect.ForgeVersionSource` and `ForgePlatformSource`;
`ModularityForge` installs both in its constructor and calls `DetectionLog.log()` there. The full
Forge runtime stays in `modularity-g420`. Gradle tasks were not run (not requested).
