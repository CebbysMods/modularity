---
# modularity-r133
title: Wire the three NeoForge @Mod classes
status: todo
type: task
priority: normal
tags:
    - neoforge
created_at: 2026-11-09T12:45:56Z
updated_at: 2026-11-09T12:46:00Z
parent: modularity-bepd
blocked_by:
    - modularity-8ff5
    - modularity-4xyy
    - modularity-co2j
    - modularity-hpj3
    - modularity-6nu3
---

Add the common @Mod class, the Dist.CLIENT class and the Dist.DEDICATED_SERVER class. Each takes IEventBus. Install the seams in the common constructor, run initCommon in FMLCommonSetupEvent, initClient in FMLClientSetupEvent and initServer in FMLDedicatedServerSetupEvent, all through enqueueWork. Keep the mod-constructor log line and add common, client and server lines.
