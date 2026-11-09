---
# modularity-8ywo
title: Verify the NeoForge lifecycle
status: todo
type: task
priority: normal
tags:
    - neoforge
    - verification
created_at: 2026-11-09T12:46:03Z
updated_at: 2026-11-09T12:46:03Z
parent: modularity-c86b
blocked_by:
    - modularity-r133
---

Run the NeoForge server smoke and then the client smoke. Check the order mod-constructor, common, initializing 0 mod(s), client, and check for no seam, class-loading, duplicate mod or UnsupportedOperationException errors. Check the server log has common and server and no client line. Record the result.
