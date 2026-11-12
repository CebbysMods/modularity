---
# modularity-ljok
title: 'quilt: mod life cycle and entrypoints'
status: completed
type: task
priority: high
created_at: 2026-11-10T21:47:39Z
updated_at: 2026-11-11T23:46:53Z
parent: modularity-gng8
---

Files: `quilt/entrypoint/ModularityQuilt{Common,Client,Server,Launch}.java`.
Work: the four entrypoints call `ModularityLoomCommon`, `ModularityLoomClient`, `ModularityLoomServer` and `ModularityLoomLaunch`; audit that each one reaches the stage it names, and that `pre_launch` runs before the game starts.
Check: the Quilt smoke log holds the four `Entrypoint ... active (quilt)` lines.

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p modularity-gradle test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). The Quilt log holds the four `Entrypoint ... active (quilt)` lines.
