---
# modularity-o6xs
title: Fabric client smoke end-to-end from the universal jar
status: todo
type: task
priority: high
tags:
    - smoke
    - fabric
created_at: 2026-11-09T07:58:53Z
updated_at: 2026-11-09T07:59:04Z
parent: modularity-xbj0
blocked_by:
    - modularity-yg81
    - modularity-up2d
    - modularity-yitg
---

First real proof that the shipped artifact loads. Run `:platforms:21.1:21.1-fabric:smokeJarClient` locally on
Windows.

## Expected in `client.txt`
- `[modularity] entrypoint main active (fabric)`
- `[modularity] entrypoint client active (fabric)`
- `[modularity] entrypoint preLaunch active (fabric)`
- `[mvd] Detected version 1.21.1 (`
- `[mpd] Detected platform FABRIC 0.20.1, CLIENT`
- `Modularity ... on Fabric, Minecraft 1.21.1: initializing 1 mod(s)`
- `Starting integrated minecraft server version 1.21.1`
- no crash, no `Mixin apply failed`, no missing dependency
- `server` entry point is **not** asserted (it does not run in a client)

## Likely findings
- The API relocation bug (ticket for universal jar) if not fixed first.
- Mixin selectors: Loom rewrites simple annotation strings to intermediary in the remapped Fabric jar, so a
  refmap may not be needed for the current seven mixins; if a selector is not rewritten, fix it or add refmap
  generation (bean modularity-dals).

## Acceptance
- The task is green from a cold cache and again from a warm cache.
- `mods/` contains exactly the universal jar, mvd-fabric, mpd-fabric and Fabric API.
