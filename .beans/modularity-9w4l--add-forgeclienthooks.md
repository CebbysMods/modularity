---
# modularity-9w4l
title: Add ForgeClientHooks
status: todo
type: task
priority: normal
tags:
    - forge
    - client
created_at: 2026-11-09T12:46:00Z
updated_at: 2026-11-09T12:46:00Z
parent: modularity-hc5n
---

Implement the client seam for Forge. Client tick uses TickEvent.ClientTickEvent (phase END) on the game bus; resource reload uses RegisterClientReloadListenersEvent on the mod bus and RunnableReloadListener. Block models and HUD warn if non-empty; key mapping, payload send/receive and entity renderer throw.
