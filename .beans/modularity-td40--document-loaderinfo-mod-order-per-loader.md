---
# modularity-td40
title: Document LoaderInfo mod order per loader
status: completed
type: task
priority: normal
tags:
    - docs
created_at: 2026-11-09T12:45:52Z
updated_at: 2026-11-09T12:50:42Z
parent: modularity-h0j3
---

State in the LoaderInfo javadoc that order is loader entrypoint order on Fabric and registration (mod construction) order on NeoForge and Forge. Do not promise a cross-loader order.

## Summary of Changes
- `LoaderInfo.modularMods` documents entrypoint order on Fabric and registration order on NeoForge/Forge. 
