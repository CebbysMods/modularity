---
# modularity-zbbp
title: 'anvil: guest mods on the Forge family'
status: todo
type: task
priority: normal
created_at: 2026-11-10T21:47:40Z
updated_at: 2026-11-11T23:47:20Z
parent: modularity-ecjq
---

Files: new `anvil/guest/**`.
Work: both loaders build mods from metadata, so a guest can join as a mod container; decide the route in docs/08 before code.
Check: the smoke log shows `Loaded guest mod smoke_guest` on NeoForge and on Forge.

## State 2026-11-11
Decision: no guest mods on the Forge family. Neither loader has a public API to add a mod after discovery, and `GuestCandidates` lives in `loom`, which the Forge tree does not stack. docs/08 records the limit. The NeoForge bean `modularity-jf4i` is scrapped for the same reason. Check: the Forge-family smokes run without the guest and expect no guest line.

## Tracker cleanup 2026-11-12

This bean is now a child of epic `modularity-ecjq`, which requires a loaded guest on every loader. The port limit written above no longer applies. The check is `Loaded guest mod smoke_guest` and `Guest mod smoke_guest: loaded=true initialized=true` in the smoke log of this loader. The Forge-family evidence is in `modularity-4bm8` and `modularity-jf4i`.

No run has proved a guest on this loader.
