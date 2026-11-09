---
# modularity-v53p
title: Add ForgeLoaderInfo
status: todo
type: task
priority: normal
tags:
    - forge
created_at: 2026-11-09T12:46:00Z
updated_at: 2026-11-09T12:46:00Z
parent: modularity-hc5n
blocked_by:
    - modularity-ibbd
---

Implement LoaderInfo for Forge: side from FMLEnvironment.dist, loaded mods and versions from ModList, development from FMLEnvironment.production, and modularMods from ModularModRegistry. Keep every ModList access lazy.
