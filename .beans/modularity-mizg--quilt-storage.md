---
# modularity-mizg
title: 'quilt: storage'
status: done
type: task
priority: normal
created_at: 2026-11-10T21:47:39Z
updated_at: 2026-11-12T12:40:00Z
parent: modularity-gng8
---

Files: `loom/loader/{ModularityLoomStorageBackend,ModularityLoomTankStorage}.java`, `loom/compat/Storages.java`.
Work: audit the droplet unit and the entity lookups on Quilt Loader.
Check: the smoke reports the tank contents in millibuckets.


Check updated 2026-11-12 (not yet run): `SmokeFeatures.storage` runs in the side stages. It builds an `McFluidTank` with `Loaders.storage().unitsPerMb()`, fills 1000 mB, drains 400 mB and logs `Smoke storage: unitsPerMb=... inserted=... units=... drained=... amount=...`. `SmokeCheck` verifies the numbers: the tank keeps exactly 1000 x unitsPerMb units (81 per mB on Fabric, 1 on the Forge family) and the portable view reports 600 mB (docs/07 7.6). Still open: `fillFromHeldItem` and `exposeEntityStorages` need a world, an item in hand and another mod reading the tank; the smoke proves the unit conversion and the portable view only.


Proved 2026-11-12 (third pass): the smoke logs `Smoke storage: unitsPerMb=81 inserted=1000 units=81000 drained=400 amount=600` on Fabric and `unitsPerMb=1 inserted=1000 units=1000 drained=400 amount=600` on the Forge family, and `SmokeCheck` verifies the arithmetic (docs/07 7.10). Still open: `fillFromHeldItem` and `exposeEntityStorages`, which need a world, an item in hand and another mod reading the tank.

## 2026-11-12: closed by the sixth run

The client smoke of quilt holds `Smoke storage: held item moved 1000 mB` and
`Smoke storage: exposed 500 mB`, so `StorageBackend.fillFromHeldItem` moved a water bucket into a tank and
`StorageBackend.exposeEntityStorages` made the entity tank reachable through the lookup of the loader
(docs/07 §7.6, §7.10). The Quilt run also found a loader constraint that affects this platform: Quilt Loader refuses a mod
entrypoint class that names client vanilla packages (`Cannot load package net.minecraft.client.server because
Mismatched @Environment`), so client-only smoke code moved into `SmokeScenario` (docs/07 §7.8). The tank
adapter fix of `modularity-dgzs` applies here too.
Open, not part of this check: a run from a shipped jar (`modularity-xbj0`).
The seventh run adds the take direction through `StorageBackend.drainToHeldItem`.
