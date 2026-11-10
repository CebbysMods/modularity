---
# modularity-xkd1
title: Verify the smoke module move
status: todo
type: task
priority: normal
tags:
    - verification
created_at: 2026-11-10T10:22:22Z
updated_at: 2026-11-10T10:22:27Z
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
