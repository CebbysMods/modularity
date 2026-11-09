---
# modularity-hpj3
title: Add NeoForge loader seam stubs
status: todo
type: task
priority: normal
tags:
    - neoforge
created_at: 2026-11-09T12:45:56Z
updated_at: 2026-11-09T12:45:56Z
parent: modularity-bepd
---

Add NeoForgeLoaderHooks, NeoForgeRenderData and NeoForgeStorageBackend. Content, payload and render methods throw UnsupportedOperationException with the method name; unitsPerMb returns 1. Do not make them silent no-ops.
