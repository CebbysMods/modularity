---
# modularity-8ywo
title: Verify the NeoForge lifecycle
status: completed
type: task
priority: normal
tags:
    - neoforge
    - verification
created_at: 2026-11-09T12:46:03Z
updated_at: 2026-11-11T23:46:53Z
parent: modularity-c86b
blocked_by:
    - modularity-r133
---

Run the NeoForge server smoke and then the client smoke. Check the order mod-constructor, common, initializing 0 mod(s), client, and check for no seam, class-loading, duplicate mod or UnsupportedOperationException errors. Check the server log has common and server and no client line. Record the result.

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p modularity-gradle test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). The NeoForge server and client smokes hold the entrypoint stage lines in order.
