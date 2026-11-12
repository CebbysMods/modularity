---
# modularity-m23h
title: Design content registration for NeoForge and Forge
status: scrapped
type: feature
priority: normal
tags:
    - neoforge
    - forge
    - registration
created_at: 2026-11-09T12:46:08Z
updated_at: 2026-11-11T23:47:00Z
parent: modularity-osig
blocked_by:
    - modularity-r133
    - modularity-g420
---

Replace the throwing LoaderHooks on NeoForge and Forge. FMLCommonSetupEvent runs after the registry events, so collect registrations during initCommon and replay them from RegisterEvent, or use DeferredRegister and change the register seam that must return a value. Include creative tabs, payloads and the block entity type. Mixins and client features stay separate.

## Tracker cleanup 2026-11-12

Scrapped as a duplicate, so the tracker has one owner for one piece of work. The throwing hooks were replaced by `modularity-d5hk` (NeoForge) and `modularity-dti9` (Forge), both completed by the 2026-11-12 run.

The bean body stays as the record.
