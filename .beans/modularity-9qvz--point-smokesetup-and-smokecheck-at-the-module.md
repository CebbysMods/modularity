---
# modularity-9qvz
title: Point SmokeSetup and SmokeCheck at the module
status: completed
type: task
priority: high
tags:
    - smoke
created_at: 2026-11-10T10:22:22Z
updated_at: 2026-11-11T23:46:54Z
parent: modularity-wo32
---

- SmokeSetup: SMOKE_ROOT = "modularity-smoke"; delete the smokeGuest source set and the smokeGuestJar
  task; depend on the ":modularity-smoke:jar" task path; guestMods points at
  modularity-smoke/build/libs/smoke_guest.jar.
- SmokeCheck and AssembleLayersTask javadoc reference the new paths.

Status: done in the working tree (not compiled; no Gradle run).

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p build-logic test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). `SmokeSetup.SMOKE_ROOT` is `modularity-smoke` and the guest jar comes from `:modularity-smoke:jar`.
