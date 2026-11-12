---
# modularity-xniw
title: Split ModularityLoomEntrypoints into Loom{Common,Client,Server,Launch} classes
status: completed
type: feature
priority: normal
tags:
    - architecture
    - loom
created_at: 2026-11-10T09:08:11Z
updated_at: 2026-11-10T21:46:45Z
parent: modularity-km3b
---

Today one class holds common/client/server/launch as static methods, while the Fabric side has ModularityFabric{Common,Client,Server,Launch} that delegate to it.

Tasks:
- [ ] Replace loom.entrypoint.ModularityLoomEntrypoints with ModularityLoomCommon, ModularityLoomClient, ModularityLoomServer, ModularityLoomLaunch
- [ ] ModularityFabric* and ModularityQuilt* call the matching class
- [ ] Remove the loader-name parameter where the detected loader already provides it
- [ ] Update docs/07 where ModularityLoomEntrypoints is described

Order: after modularity-km3b Phase 6, or adjust the call site if done earlier.

Done 2026-11-10 on branch feature/port-loom: `ModularityLoomCommon`, `ModularityLoomClient`, `ModularityLoomServer` and `ModularityLoomLaunch` replace `ModularityLoomEntrypoints`, with `ModularityLoomSeams` for the one install both the common and the launch stage call. The eight Fabric entrypoints and the eight Quilt entrypoints call the matching class. The loader-name parameter stays: it names the loader in the `Entrypoint ... active` line before detection runs, and a detected name would change that log line without a run to prove it. docs/06 rows updated; docs/07 has no `ModularityLoomEntrypoints` text. Check: javac compiled the loom, fabric and quilt sources against the 1.21.1 mapped jar and the Fabric, Quilt and Allure class paths with no error. No Gradle run (hard rule 1).
