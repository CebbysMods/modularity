---
# modularity-osig
title: Wire the NeoForge and Forge lifecycle
status: completed
type: epic
priority: normal
tags:
    - lifecycle
    - neoforge
    - forge
created_at: 2026-11-09T12:45:27Z
updated_at: 2026-11-11T23:47:02Z
---

Start Modularity's runtime on NeoForge 21.1.265 and Forge 52.1.21: install the loader seams and run initCommon, initClient and initServer at the loader lifecycle points. Scope is lifecycle only. Content registration, payloads, capabilities, render data and client features stay unimplemented and must fail with a clear message. Subepics: shared runtime foundation, NeoForge, Forge, verification and follow-ups.

## Tracker cleanup 2026-11-12

Closed as completed: every child bean is closed and the exit check ran green on 2026-11-12 (`-p build-logic test`, the eight 21.1 smokes, docs/07 7.10). The lifecycle it planned, on both loaders, is proved in the logs.
