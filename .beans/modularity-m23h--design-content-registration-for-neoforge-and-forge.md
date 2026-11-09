---
# modularity-m23h
title: Design content registration for NeoForge and Forge
status: todo
type: feature
priority: normal
tags:
    - neoforge
    - forge
    - registration
created_at: 2026-11-09T12:46:08Z
updated_at: 2026-11-09T12:46:08Z
parent: modularity-osig
blocked_by:
    - modularity-r133
    - modularity-g420
---

Replace the throwing LoaderHooks on NeoForge and Forge. FMLCommonSetupEvent runs after the registry events, so collect registrations during initCommon and replay them from RegisterEvent, or use DeferredRegister and change the register seam that must return a value. Include creative tabs, payloads and the block entity type. Mixins and client features stay separate.
