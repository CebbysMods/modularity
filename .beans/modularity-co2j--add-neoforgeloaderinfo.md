---
# modularity-co2j
title: Add NeoForgeLoaderInfo
status: completed
type: task
priority: normal
tags:
    - neoforge
created_at: 2026-11-09T12:45:56Z
updated_at: 2026-11-09T13:29:56Z
parent: modularity-bepd
blocked_by:
    - modularity-ibbd
---

Implement LoaderInfo for NeoForge: side from FMLEnvironment.dist, loaded mods and versions from ModList, development from FMLEnvironment.production, and modularMods from ModularModRegistry. Keep every ModList access lazy (common setup or later).
