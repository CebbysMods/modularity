---
# modularity-g420
title: Wire the Forge @Mod and client subscriber
status: completed
type: task
priority: normal
tags:
    - forge
created_at: 2026-11-09T12:46:00Z
updated_at: 2026-11-11T23:46:53Z
parent: modularity-hc5n
blocked_by:
    - modularity-8ff5
    - modularity-4xyy
    - modularity-v53p
    - modularity-r870
    - modularity-9w4l
---

Take FMLJavaModLoadingContext in the @Mod constructor, install the seams and run initCommon in FMLCommonSetupEvent and initServer in FMLDedicatedServerSetupEvent through enqueueWork. Add a Dist.CLIENT @Mod.EventBusSubscriber for FMLClientSetupEvent that installs the client hooks and runs ModularityClient.init. Keep the mod-constructor log line.

## Risks to check
- Forge 52.1.21 injects only FMLJavaModLoadingContext; use context.getModEventBus(). Do not copy the NeoForge constructor signature and do not use FMLModLoadingContext (it does not exist in this version).
- Client tick is a game-bus event; setup and reload events are mod-bus events. Verify the handlers run.
- Keep the line "entrypoint mod-constructor active (forge)" verbatim.

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p build-logic test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). `ModularityForge` is in the tree and the log holds `Entrypoint mod-constructor active (forge)`.
