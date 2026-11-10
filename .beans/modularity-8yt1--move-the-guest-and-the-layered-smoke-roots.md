---
# modularity-8yt1
title: Move the guest and the layered smoke roots
status: todo
type: task
priority: high
tags:
    - smoke
created_at: 2026-11-10T10:22:22Z
updated_at: 2026-11-10T10:22:22Z
parent: modularity-wo32
---

- git mv tools/smoke/guest/src/main/{java,resources} to modularity-smoke/src/main/{java,resources}.
- git mv tools/smoke/{minecraft,loom,fabric,quilt,neoforge,forge} modularity-smoke/.
- Delete tools/smoke. tools/smoke-fabric-1.21.1 stays in tools/.

Status: done in the working tree; no content changes.
