---
# modularity-4l2a
title: 'forge: guest mods'
status: todo
type: task
priority: normal
created_at: 2026-11-10T21:47:42Z
updated_at: 2026-11-11T23:47:20Z
parent: modularity-ecjq
---

Files: new `forge/guest/**`.
Work: no guest support today; the route is the same as NeoForge, and docs/08 decides it first.
Check: the smoke log shows `Loaded guest mod smoke_guest`.

Not supported on Forge, as on NeoForge. FML has no public API to inject a guest as a real mod, so no guest is discovered, injected or run. docs/08 states the limit. The bean stays open until the owner decides whether the Forge family gets a scan that at least names the guests it cannot load.

## Tracker cleanup 2026-11-12

This bean is now a child of epic `modularity-ecjq`, which requires a loaded guest on every loader. The port limit written above no longer applies. The check is `Loaded guest mod smoke_guest` and `Guest mod smoke_guest: loaded=true initialized=true` in the smoke log of this loader. The Forge-family evidence is in `modularity-4bm8` and `modularity-jf4i`.

No run has proved a guest on this loader.
