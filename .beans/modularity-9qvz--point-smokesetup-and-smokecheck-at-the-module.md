---
# modularity-9qvz
title: Point SmokeSetup and SmokeCheck at the module
status: todo
type: task
priority: high
tags:
    - smoke
created_at: 2026-11-10T10:22:22Z
updated_at: 2026-11-10T10:22:22Z
parent: modularity-wo32
---

- SmokeSetup: SMOKE_ROOT = "modularity-smoke"; delete the smokeGuest source set and the smokeGuestJar
  task; depend on the ":modularity-smoke:jar" task path; guestMods points at
  modularity-smoke/build/libs/smoke_guest.jar.
- SmokeCheck and AssembleLayersTask javadoc reference the new paths.

Status: done in the working tree (not compiled; no Gradle run).
