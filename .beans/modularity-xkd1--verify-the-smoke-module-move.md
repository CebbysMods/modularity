---
# modularity-xkd1
title: Verify the smoke module move
status: completed
type: task
priority: normal
tags:
    - verification
created_at: 2026-11-10T10:22:22Z
updated_at: 2026-11-11T23:46:54Z
parent: modularity-wo32
blocked_by:
    - modularity-p6y0
    - modularity-8yt1
    - modularity-9qvz
    - modularity-y07y
---

Run on request: `-p build-logic test`, `:modularity-smoke:jar`, the Fabric smoke (guest lines
present), the Quilt smoke (no guest line). Check that smoke_guest.jar holds modularity.mod.json and
SmokeGuest.class only.

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p build-logic test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). `smoke_guest.jar` holds `modularity.mod.json` and the `SmokeGuest` classes only, the Fabric log holds the guest lines and the Quilt log holds none.
