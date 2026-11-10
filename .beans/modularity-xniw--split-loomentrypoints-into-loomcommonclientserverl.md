---
# modularity-xniw
title: Split LoomEntrypoints into Loom{Common,Client,Server,Launch} classes
status: todo
type: feature
priority: normal
tags:
    - architecture
    - loom
created_at: 2026-11-10T09:08:11Z
updated_at: 2026-11-10T09:08:11Z
parent: modularity-km3b
---

Today one class holds common/client/server/launch as static methods, while the Fabric side has ModularityFabric{Common,Client,Server,Launch} that delegate to it.

Tasks:
- [ ] Replace loom.entrypoint.LoomEntrypoints with LoomCommon, LoomClient, LoomServer, LoomLaunch
- [ ] ModularityFabric* and ModularityQuilt* call the matching class
- [ ] Remove the loader-name parameter where the detected loader already provides it
- [ ] Update docs/07 where LoomEntrypoints is described

Order: after modularity-km3b Phase 6, or adjust the call site if done earlier.
