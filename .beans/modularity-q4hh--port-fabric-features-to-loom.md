---
# modularity-q4hh
title: Port Fabric features to loom
status: in-progress
type: epic
priority: normal
tags:
    - port
    - loom
created_at: 2026-11-10T21:43:45Z
updated_at: 2026-11-10T21:46:45Z
---

Stage 1 of the port programme. Fabric is the reference. This epic makes `platforms/loom` the one home of the Fabric-family feature set, so `platforms/fabric` stays a thin entrypoint root and Quilt gets the same behaviour with no copy.

Branch: `feature/port-loom`.

Steps: one task per functionality area — mod lifecycle and entrypoints; detection; content registration; networking; storage; client; mixins; guest mods; verification.

Exit check: the `loom` audit table in docs/07 §7.3 marks each area as implemented in `loom`, and `-p modularity-gradle test` plus the 21.1 Fabric smoke pass. Gradle runs wait for the owner's word.

2026-11-10: stage opened on branch `feature/port-loom`. Task beans: `modularity-pnv1` lifecycle, `modularity-xt51` detection, `modularity-8yym` registration, `modularity-7kd6` networking, `modularity-dgzs` storage, `modularity-dga9` client, `modularity-bggb` mixins, `modularity-p999` guests, `modularity-l2mx` verification. The audit table is in docs/07 §7.2.

## Delta against Fabric, 2026-11-12

Fabric is the reference (95). Its own two open checks are a rendered dynamic-model frame and a load from the
shipped jar. Every Fabric feature reaches Quilt except guest mods (docs/08). PORT-REVIEW carries the table.
