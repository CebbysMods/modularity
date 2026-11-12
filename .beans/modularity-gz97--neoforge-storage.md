---
# modularity-gz97
title: 'neoforge: storage'
status: done
type: task
priority: normal
created_at: 2026-11-10T21:47:41Z
updated_at: 2026-11-12T12:40:00Z
parent: modularity-yrjc
---

Files: `neoforge/loader/NeoForgeStorageBackend.java`.
Work: `exposeEntityStorages` and `fillFromHeldItem` throw `notImplemented`; `unitsPerMb` returns 1. Use the NeoForge capability API with `RegisterCapabilitiesEvent`.
Check: the smoke reports the tank contents and fills a tank from a held bucket.

## Audit 2026-11-11 (stage 4)
In the tree: `NeoForgeStorageBackend(IEventBus)` exposes `Capabilities.FluidHandler.ENTITY` and `Capabilities.ItemHandler.ENTITY` through `RegisterCapabilitiesEvent`; `NeoForgeTankHandler` maps `McFluidTank` to `IFluidHandler` (millibuckets); `fillFromHeldItem` uses `FluidUtil.tryEmptyContainerAndStow`. No Gradle run: AGENTS.md hard rule 1. Verification stays open in the verification bean.


Check updated 2026-11-12 (not yet run): `SmokeFeatures.storage` runs in the side stages. It builds an `McFluidTank` with `Loaders.storage().unitsPerMb()`, fills 1000 mB, drains 400 mB and logs `Smoke storage: unitsPerMb=... inserted=... units=... drained=... amount=...`. `SmokeCheck` verifies the numbers: the tank keeps exactly 1000 x unitsPerMb units (81 per mB on Fabric, 1 on the Forge family) and the portable view reports 600 mB (docs/07 7.6). Still open: `fillFromHeldItem` and `exposeEntityStorages` need a world, an item in hand and another mod reading the tank; the smoke proves the unit conversion and the portable view only.


Proved 2026-11-12 (third pass): the smoke logs `Smoke storage: unitsPerMb=81 inserted=1000 units=81000 drained=400 amount=600` on Fabric and `unitsPerMb=1 inserted=1000 units=1000 drained=400 amount=600` on the Forge family, and `SmokeCheck` verifies the arithmetic (docs/07 7.10). Still open: `fillFromHeldItem` and `exposeEntityStorages`, which need a world, an item in hand and another mod reading the tank.

## 2026-11-12: closed by the sixth run

The client smoke of neoforge holds `Smoke storage: held item moved 1000 mB` and
`Smoke storage: exposed 500 mB`, so `StorageBackend.fillFromHeldItem` moved a water bucket into a tank and
`StorageBackend.exposeEntityStorages` made the entity tank reachable through the lookup of the loader
(docs/07 §7.6, §7.10). NeoForge needed no extra fix: the item path and the entity capability both worked with the shared smoke code.
Open, not part of this check: a run from a shipped jar (`modularity-xbj0`).
The seventh run adds the take direction through `StorageBackend.drainToHeldItem`, and fixes the handler that read the fluid after draining.
