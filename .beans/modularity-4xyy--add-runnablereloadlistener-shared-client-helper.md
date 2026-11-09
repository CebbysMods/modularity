---
# modularity-4xyy
title: Add RunnableReloadListener shared client helper
status: completed
type: task
priority: normal
tags:
    - client
created_at: 2026-11-09T12:45:52Z
updated_at: 2026-11-09T12:50:42Z
parent: modularity-h0j3
---

Add a PreparableReloadListener that runs a no-argument action on the game thread after a client resource reload. Both loaders use it for ClientHooks.onResourceReload.

## Summary of Changes
- Added `platform.client.RunnableReloadListener` (`PreparableReloadListener`, runs the action on the game executor). 
