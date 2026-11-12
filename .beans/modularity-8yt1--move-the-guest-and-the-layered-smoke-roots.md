---
# modularity-8yt1
title: Move the guest and the layered smoke roots
status: completed
type: task
priority: high
tags:
    - smoke
created_at: 2026-11-10T10:22:22Z
updated_at: 2026-11-11T23:46:54Z
parent: modularity-wo32
---

- git mv tools/smoke/guest/src/main/{java,resources} to modularity-smoke/src/main/{java,resources}.
- git mv tools/smoke/{minecraft,loom,fabric,quilt,neoforge,forge} modularity-smoke/.
- Delete tools/smoke. tools/smoke-fabric-1.21.1 stays in tools/.

Status: done in the working tree; no content changes.

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p build-logic test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). The roots and the guest sources are under `modularity-smoke` and `tools/smoke` is gone.
