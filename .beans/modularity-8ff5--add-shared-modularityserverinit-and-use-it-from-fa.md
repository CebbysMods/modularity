---
# modularity-8ff5
title: Add shared ModularityServer.init and use it from Fabric
status: completed
type: task
priority: normal
tags:
    - runtime
created_at: 2026-11-09T12:45:52Z
updated_at: 2026-11-09T12:50:42Z
parent: modularity-h0j3
---

Move the ServerContext record and the initServer loop from ModularityFabricServer into a shared platform helper. The Fabric dedicated-server entry point keeps the same behaviour.

## Summary of Changes
- Added `platform.ModularityServer` with the `ServerContext` record.
- `ModularityFabricServer` now calls `ModularityServer.init()`. 
