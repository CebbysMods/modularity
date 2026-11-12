---
# modularity-81nl
title: 'quilt: detection and the log line'
status: completed
type: task
priority: high
created_at: 2026-11-10T21:47:39Z
updated_at: 2026-11-11T23:46:53Z
parent: modularity-gng8
---

Files: `loom/detect/Loom{Classes,PlatformSource,VersionSource}.java`.
Work: `ModularityLoomPlatformSource` must report `QUILT` when `org.quiltmc.loader.api.QuiltLoader` is present.
Check: the smoke log holds `platform quilt:<loader>, minecraft:<version>`.

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p build-logic test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). The Quilt log holds `(server) platform quilt:0.30.2, minecraft:1.21.1`.
