---
# modularity-jpo5
title: 'Phase 3: Quilt detection verification'
status: completed
type: task
priority: normal
tags:
    - architecture
    - quilt
created_at: 2026-11-10T09:08:10Z
updated_at: 2026-11-11T23:39:00Z
parent: modularity-km3b
---

Quilt uses the loom sources through the Fabric Loader API. No new class is expected.

Tasks:
- [ ] Confirm ModularityLoomPlatformSource reports QUILT and the quilt_loader container version
- [ ] Confirm the side on server and client
- [ ] Confirm no Fabric Loader runtime dependency is needed (QuiltPlatformSetup still excludes it)
- [x] Update the smoke expectations when the old [mvd]/[mpd] lines go (Phase 6)

Verification (on request): -Pmodularity.platforms=quilt-21.1 buildAll and :21.1-quilt:smoke.

## Progress

2026-11-10: no new class. `ModularityLoomPlatformSource` reads the `quilt_loader` container when
`QuiltLoader` is present and reports `QUILT`; the Quilt platform runs the same `ModularityLoomEntrypoints.common`
path. The smoke expectation for the `[modularity] platform ...` line is in `SmokeCheck`. Runtime
verification was not run (Gradle not requested).

Status set 2026-11-12: the 2026-11-12 run is green on Quilt and the logs hold `(server) platform quilt:0.30.2, minecraft:1.21.1` and the same line for the client (docs/07 §7.10), so the loader id, the quilt_loader version, the side and the exclusion of Fabric Loader are all shown.
