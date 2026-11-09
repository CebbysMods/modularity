---
# modularity-g420
title: Wire the Forge @Mod and client subscriber
status: todo
type: task
priority: normal
tags:
    - forge
created_at: 2026-11-09T12:46:00Z
updated_at: 2026-11-09T12:46:03Z
parent: modularity-hc5n
blocked_by:
    - modularity-8ff5
    - modularity-4xyy
    - modularity-v53p
    - modularity-r870
    - modularity-9w4l
---

Take FMLJavaModLoadingContext in the @Mod constructor, install the seams and run initCommon in FMLCommonSetupEvent and initServer in FMLDedicatedServerSetupEvent through enqueueWork. Add a Dist.CLIENT @Mod.EventBusSubscriber for FMLClientSetupEvent that installs the client hooks and runs ModularityClient.init. Keep the mod-constructor log line.
