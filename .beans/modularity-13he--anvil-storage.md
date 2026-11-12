---
# modularity-13he
title: 'anvil: storage'
status: scrapped
type: task
priority: normal
created_at: 2026-11-10T21:47:40Z
updated_at: 2026-11-11T23:47:02Z
parent: modularity-arii
---

Files: new `anvil/loader/**` storage classes.
Work: the Forge-family capability API counts millibuckets, so `unitsPerMb` is 1 on both loaders; share the capability provider and keep the registration event in the loader.
Check: the smoke reports the tank contents in millibuckets.

## State 2026-11-11
`anvil/loader/**` storage did not exist: `unitsPerMb()` returns 1 in `NeoForgeStorageBackend` and `ForgeStorageBackend`; the entity tank is `NeoForgeTankHandler` and `ForgeTankHandler` (millibuckets, `FluidUtil.tryEmptyContainerAndStow`); the entity item storage is `ForgeEntityStorages` on Forge and the capability provider in `NeoForgeLoaderHooks` on NeoForge. `javac` exit 0. No Gradle run. Check: the smoke reports the tank contents in millibuckets.

## Tracker cleanup 2026-11-12

Scrapped as a duplicate, so the tracker has one owner for one piece of work. Storage did not land in `anvil`: the capabilities are per loader, `modularity-gz97` (NeoForge) and `modularity-tll2` (Forge).

The bean body stays as the record.
