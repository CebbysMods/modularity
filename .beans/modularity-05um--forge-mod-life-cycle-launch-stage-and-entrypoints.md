---
# modularity-05um
title: 'forge: mod life cycle, launch stage and entrypoints'
status: completed
type: task
priority: high
created_at: 2026-11-10T21:47:41Z
updated_at: 2026-11-11T23:38:49Z
parent: modularity-cuuj
---

Files: `forge/ModularityForge.java`.
Work: the constructor installs detection and logs one line. Add the seams install, `SyncedData.init`, `ModularityCommon.init`, the launch stage, and the client and server classes for `Dist` with `ModularityClient` and `ModularityServer`.
Check: the smoke log holds the four stage lines.

Done in the tree (no run yet). `ModularityForge` takes `FMLJavaModLoadingContext`, and runs `ModularityAnvilLifecycle.detect`, `EntryPoints.active("mod-constructor", "forge")`, `launch`, `launchMods` and `common` in that order, then adds `ModularityForgeClient` or `ModularityForgeServer` by `FMLEnvironment.dist`. The client and server classes run the client and server stages in `enqueueWork` at `FMLClientSetupEvent` and `FMLDedicatedServerSetupEvent`. Risk: the order is unproven; only a `javac` compile of 97 sources ran. Check stays open: the smoke log must hold the four stage lines.

Status set 2026-11-12: the 2026-11-12 smoke run is green and every Forge log holds the mod-constructor, launch, common and side stage lines; the smoke check requires all four. See docs/07 §7.10.
