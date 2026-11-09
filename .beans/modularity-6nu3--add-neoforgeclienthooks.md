---
# modularity-6nu3
title: Add NeoForgeClientHooks
status: completed
type: task
priority: normal
tags:
    - neoforge
    - client
created_at: 2026-11-09T12:45:56Z
updated_at: 2026-11-09T13:29:56Z
parent: modularity-bepd
---

Implement the client seam for NeoForge. Client tick uses ClientTickEvent.Post on the game bus; resource reload uses RegisterClientReloadListenersEvent on the mod bus and RunnableReloadListener. Block models and HUD warn if non-empty; key mapping, payload send/receive and entity renderer throw.
