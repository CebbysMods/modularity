---
# modularity-dgzs
title: 'loom: storage'
status: done
type: task
priority: normal
created_at: 2026-11-10T21:44:12Z
updated_at: 2026-11-12T12:40:00Z
parent: modularity-q4hh
---

Files: `loom/loader/{ModularityLoomStorageBackend,ModularityLoomTankStorage}.java`, `loom/compat/Storages.java`.
Work: audit the droplet unit (81 per millibucket), the entity lookups and the fill from the held item.
Check: the smoke reports the tank contents in millibuckets.

Audit 2026-11-10: `ModularityLoomStorageBackend` returns droplets (81 per millibucket), `ModularityLoomTankStorage` bridges `McFluidTank`, `compat.Storages` wraps a container; the entity lookups are Modularity own. No stub.



2026-11-10 (branch feature/port-loom): stage 1 audit. The work is in the tree. `grep` finds no `UnsupportedOperationException` and no `notImplemented` under `platforms/loom`. The Fabric root now holds only `fabric/entrypoint` and `fabric/guest`, so each functionality lives in one place. `javac` compiled the loom, fabric and quilt sources against the 1.21.1 mapped jar and the Fabric, Quilt and Allure class paths with no error. No Gradle run (hard rule 1), so every smoke check below stays open: `-p modularity-gradle test`, `-Pmodularity.platforms=fabric-21.1 :platforms:21.1:smoke`, `-Pmodularity.platforms=quilt-21.1 :platforms:21.1:smoke`.
Specific: `ModularityLoomStorageBackend` reports 81000 units per millibucket and uses `Storages`.


Check updated 2026-11-12 (not yet run): `SmokeFeatures.storage` runs in the side stages. It builds an `McFluidTank` with `Loaders.storage().unitsPerMb()`, fills 1000 mB, drains 400 mB and logs `Smoke storage: unitsPerMb=... inserted=... units=... drained=... amount=...`. `SmokeCheck` verifies the numbers: the tank keeps exactly 1000 x unitsPerMb units (81 per mB on Fabric, 1 on the Forge family) and the portable view reports 600 mB (docs/07 7.6). Still open: `fillFromHeldItem` and `exposeEntityStorages` need a world, an item in hand and another mod reading the tank; the smoke proves the unit conversion and the portable view only.


Proved 2026-11-12 (third pass): the smoke logs `Smoke storage: unitsPerMb=81 inserted=1000 units=81000 drained=400 amount=600` on Fabric and `unitsPerMb=1 inserted=1000 units=1000 drained=400 amount=600` on the Forge family, and `SmokeCheck` verifies the arithmetic (docs/07 7.10). Still open: `fillFromHeldItem` and `exposeEntityStorages`, which need a world, an item in hand and another mod reading the tank.

## 2026-11-12: closed by the sixth run

The client smoke of loom holds `Smoke storage: held item moved 1000 mB` and
`Smoke storage: exposed 500 mB`, so `StorageBackend.fillFromHeldItem` moved a water bucket into a tank and
`StorageBackend.exposeEntityStorages` made the entity tank reachable through the lookup of the loader
(docs/07 §7.6, §7.10). The run also fixed a defect in this layer: `ModularityLoomTankStorage` passed a null transaction into
`SnapshotParticipant`, which throws, so the adapter now applies the change and notifies when the context is
absent (docs/07 §7.8). Quilt runs the same class, so `modularity-mizg` shares the fix.
Open, not part of this check: a run from a shipped jar (`modularity-xbj0`).
The seventh run adds the take direction through `StorageBackend.drainToHeldItem`.
