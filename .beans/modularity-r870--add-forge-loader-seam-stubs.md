---
# modularity-r870
title: Add Forge loader seam stubs
status: completed
type: task
priority: normal
tags:
    - forge
created_at: 2026-11-09T12:46:00Z
updated_at: 2026-11-11T23:46:53Z
parent: modularity-hc5n
---

Add ForgeLoaderHooks, ForgeRenderData and ForgeStorageBackend. Content, payload and render methods throw UnsupportedOperationException with the method name; unitsPerMb returns 1. Do not make them silent no-ops.

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p build-logic test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). `ForgeLoaderHooks`, `ForgeRenderData` and `ForgeStorageBackend` are in the tree and the Forge smoke is green.
