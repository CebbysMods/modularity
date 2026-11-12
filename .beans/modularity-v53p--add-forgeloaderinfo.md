---
# modularity-v53p
title: Add ForgeLoaderInfo
status: completed
type: task
priority: normal
tags:
    - forge
created_at: 2026-11-09T12:46:00Z
updated_at: 2026-11-11T23:46:53Z
parent: modularity-hc5n
blocked_by:
    - modularity-ibbd
---

Implement LoaderInfo for Forge: side from FMLEnvironment.dist, loaded mods and versions from ModList, development from FMLEnvironment.production, and modularMods from ModularModRegistry. Keep every ModList access lazy.

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p build-logic test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). `ForgeLoaderInfo` is in the tree and the Forge detection line is in the log.
