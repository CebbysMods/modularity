---
# modularity-jpo5
title: 'Phase 3: Quilt detection verification'
status: todo
type: task
priority: normal
tags:
    - architecture
    - quilt
created_at: 2026-11-10T09:08:10Z
updated_at: 2026-11-10T09:08:10Z
parent: modularity-km3b
---

Quilt uses the loom sources through the Fabric Loader API. No new class is expected.

Tasks:
- [ ] Confirm LoomPlatformSource reports QUILT and the quilt_loader container version
- [ ] Confirm the side on server and client
- [ ] Confirm no Fabric Loader runtime dependency is needed (QuiltPlatformSetup still excludes it)
- [x] Update the smoke expectations when the old [mvd]/[mpd] lines go (Phase 6)

Verification (on request): -Pmodularity.platforms=quilt-21.1 buildAll and :21.1-quilt:smoke.

## Progress

2026-11-10: no new class. `LoomPlatformSource` reads the `quilt_loader` container when
`QuiltLoader` is present and reports `QUILT`; the Quilt platform runs the same `LoomEntrypoints.common`
path. The smoke expectation for the `[modularity] platform ...` line is in `SmokeCheck`. Runtime
verification was not run (Gradle not requested).
