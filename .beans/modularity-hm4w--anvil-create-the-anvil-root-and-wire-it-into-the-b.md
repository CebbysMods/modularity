---
# modularity-hm4w
title: 'anvil: create the anvil root and wire it into the build'
status: completed
type: task
priority: high
created_at: 2026-11-10T21:47:40Z
updated_at: 2026-11-11T23:46:53Z
parent: modularity-arii
---

Files: `platforms/anvil/common/src/main/java/lv/cebbys/mcmods/modularity/anvil/`, the Forge-family platform setup in `modularity-gradle/.../setup/`, and the roots list it builds.
Work: add the root, give it the package `lv.cebbys.mcmods.modularity.anvil`, and stack it above `common` and below `neoforge` and `forge`.
Check: `-p modularity-gradle test`; the layer list of a NeoForge platform contains `anvil`.

## Audit 2026-11-11 (stage 3)
In the tree: `platforms/anvil/common/src/main/java/.../anvil/` holds `ModularityAnvilLifecycle`, `ModularityAnvilSeams`, `ModularityAnvilLoaderApi`, `ModularityAnvilPlatformSource` and `ModularityAnvilVersionSource`. `Loader.java` stacks `minecraft, common, anvil, neoforge` and `minecraft, common, anvil, forge`; `TableAndMetadataTest` asserts the NeoForge stack. The layer compiles with `javac` with neither loader on the class path. No Gradle run: AGENTS.md hard rule 1. Verification stays open.

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p modularity-gradle test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). The layer list of both Forge-family platforms holds `anvil`.
