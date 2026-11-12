---
# modularity-yfl7
title: Native Quilt entry points (quilt.mod.json)
status: scrapped
type: task
priority: normal
tags:
    - quilt
    - loader
created_at: 2026-11-08T19:48:16Z
updated_at: 2026-11-11T23:47:30Z
---

The universal jar currently covers Quilt through Quilt Loader's Fabric compatibility (fabric.mod.json + Fabric
entry points), which is how the quilt platform works today.

Making it a native Quilt mod needs:
- `quilt.mod.json` with Quilt entry points (`org.quiltmc.loader.api.entrypoint.*`) instead of the Fabric ones.
- Quilt implementations of the seams the Fabric entry points install: modularity loader info, render data, storage
  backend, loader hooks (preLaunch/guest handling), client hooks - the Fabric versions reference Fabric Loader API
  classes that do not exist under a native Quilt runtime.
- A Quilt-native dependency chain (no `fabric-api` dependency).

## Acceptance
- [ ] platforms/quilt/common holds the Quilt entry points and seams
- [ ] quilt.mod.json generated with those entry points
- [ ] Verified on a real Quilt install

## Tracker cleanup 2026-11-12

Scrapped as a duplicate. The native Quilt entry points are `ljok` (completed), and `quilt.mod.json` carries
them in the universal jar (docs/07 7.10). The second half of this bean was decided against: Quilt uses Fabric
API as its library, so there is no Quilt-native seam set and no separate dependency chain (docs/00, known
limits).
