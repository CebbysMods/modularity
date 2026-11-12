---
# modularity-lw0r
title: Verify the Forge lifecycle
status: completed
type: task
priority: normal
tags:
    - forge
    - verification
created_at: 2026-11-09T12:46:03Z
updated_at: 2026-11-11T23:46:54Z
parent: modularity-c86b
blocked_by:
    - modularity-g420
---

Run the Forge server smoke and then the client smoke. Check the same entry-point order and the same absence of errors. Check the server log has common and server and no client line. Record the result.

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p build-logic test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). The Forge server and client smokes hold the entrypoint stage lines in order.
