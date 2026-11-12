---
# modularity-igmc
title: 'Detection sources: create once per loader and keep the detected data'
status: todo
type: task
priority: normal
tags:
    - architecture
    - detection
created_at: 2026-11-12T14:31:22Z
updated_at: 2026-11-12T14:31:22Z
parent: modularity-km3b
---

Today each loader installs its detection sources twice: the launch stage and the common stage. On Fabric and Quilt each entrypoint also constructs its own pair, so one loader builds four source objects (two pairs) for one platform source and one version source. The Forge family holds one pair in `ModularityAnvilServices`, so it builds two.

`PlatformDetector.install` and `VersionDetector.install` key by `name()` and replace the entry, but they also clear the cached result. The second install in the common stage therefore runs the detection a second time.

The values cannot change during a run: the loader, the loader version, the physical side, the development flag and the Minecraft version are fixed at launch. Detect them once and let every later reader use the same result.

Goal:
- One platform source and one version source per loader, created once.
- One install per loader, in its earliest entrypoint (Fabric `preLaunch`, Quilt `pre_launch`, the NeoForge and Forge mod constructor).
- `Loaders` seams stay installed before `ModularityCommon.init`, also on a loader with no launch stage.
- `DetectionLog.log()` and `LaunchContext.side()` read the cached result, not a fresh detection.

Files: `platforms/loom/.../entrypoint/ModularityLoomServices.java`, `ModularityLoomCommon`, `ModularityLoomLaunch`, `platforms/anvil/.../ModularityAnvilServices.java`, the eight loader entrypoints, `platforms/common/.../platform/detect/DetectionLog.java`.

Open choice: install detection in the launch stage only and keep `Loaders.install` in the common stage; or keep both install points and reuse one held pair of sources. Decide with the owner before the change.

Verification (on request, AGENTS.md hard rule 1): the 21.1 smokes on the four loaders hold `(server|client) platform <loader>:<loaderVersion>, minecraft:1.21.1`, and the log holds one detection line per run.
