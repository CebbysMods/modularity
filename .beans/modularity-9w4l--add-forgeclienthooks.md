---
# modularity-9w4l
title: Add ForgeClientHooks
status: completed
type: task
priority: normal
tags:
    - forge
    - client
created_at: 2026-11-09T12:46:00Z
updated_at: 2026-11-11T23:46:53Z
parent: modularity-hc5n
---

Implement the client seam for Forge. Client tick uses TickEvent.ClientTickEvent (phase END) on the game bus; resource reload uses RegisterClientReloadListenersEvent on the mod bus and RunnableReloadListener. Block models and HUD warn if non-empty; key mapping, payload send/receive and entity renderer throw.

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p build-logic test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). `ForgeClientHooks` is in the tree; the Forge client smoke joined the world and wrote its screenshot.
