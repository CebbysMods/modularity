---
# modularity-up2d
title: Log every entry point on all four loaders
status: completed
type: task
priority: high
tags:
    - entrypoints
    - logging
created_at: 2026-11-09T07:58:35Z
updated_at: 2026-11-09T08:16:57Z
parent: modularity-xbj0
---

A log line must prove which entry point the loader reached. Add one shared helper so the line format is uniform
and safe before the loader seams are installed.

## Work
- New `EntryPoints.active(entryPoint, loader)` under
  `platforms/minecraft/common/src/main/java/lv/cebbys/mcmods/modularity/platform/`, logging
  `[modularity] entrypoint <name> active (<loader>)`. No dependency on `Loaders` or `McRuntime`.
- Call it first thing in:
  - Fabric: `ModularityFabricCommon.onInitialize` -> `common`; `ModularityFabricClient.onInitializeClient` -> `client`;
    `ModularityFabricServer.onInitializeServer` -> `server`; `guest/GuestMods.onPreLaunch` -> `launch`
  - Forge: `ModularityForge` constructor -> `mod-constructor`
  - NeoForge: `ModularityNeoForgeCommon` constructor -> `mod-constructor`
- Pass `McRuntime.INSTANCE.loader()` to the existing `ModularityCore.initCommon` summary so Quilt reports `QUILT`
  instead of `Fabric`.

## Notes
- Forge/NeoForge still do not start the core (beans modularity-vk1z / modularity-dals); the smoke records that
  instead of asserting it.
- The Fabric `server` entry point does not run in a client launch; it is not asserted by the smoke.

## Acceptance
- All four loaders log their entry-point lines in a real client run.
- Existing `smokeAll` output is unchanged apart from the added lines.
