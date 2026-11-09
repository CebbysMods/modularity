---
# modularity-r870
title: Add Forge loader seam stubs
status: todo
type: task
priority: normal
tags:
    - forge
created_at: 2026-11-09T12:46:00Z
updated_at: 2026-11-09T12:46:00Z
parent: modularity-hc5n
---

Add ForgeLoaderHooks, ForgeRenderData and ForgeStorageBackend. Content, payload and render methods throw UnsupportedOperationException with the method name; unitsPerMb returns 1. Do not make them silent no-ops.
