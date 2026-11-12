---
# modularity-0pwp
title: Verify the split detection sources with the smoke of all four loaders
status: in-progress
type: task
priority: normal
tags:
    - architecture
    - detection
created_at: 2026-11-12T13:49:52Z
updated_at: 2026-11-12T13:49:52Z
---

Each detection source now implements `detect()` itself. `platform.detect` holds only `AbstractModularityPlatformSource`/`AbstractModularityVersionSource` (priority 1000); the `loom` bases hold the Fabric Loader calls that Fabric and Quilt share; one child per loader (`FabricSource`, `ModularityQuiltPlatformSource`, `ModularityForgePlatformSource`, `ModularityNeoForgePlatformSource` and the four version sources) supplies the loader and its API calls. The Quilt sources report `quilt`; the anvil children call their own FML instead of the deleted reflection helper.

Tasks:
- [ ] Compile all detection classes (javac): done, the two bases, the two loom sources and the four Loom children against the Fabric Loader jar, and the four Forge-family children against the FML jars
- [ ] Run the smoke of 21.1 on Fabric, Quilt, NeoForge and Forge, server and client
- [ ] Check each log for `(server|client) platform <loader>:<loaderVersion>, minecraft:1.21.1`
- [ ] Check that a Quilt failure names `quilt` and a Forge failure names `forge`

Verification (on request): `./gradlew -Pmodularity.platforms=fabric-21.1,quilt-21.1,neoforge-21.1,forge-21.1 :platforms:21.1:21.1-fabric:smoke :platforms:21.1:21.1-quilt:smoke :platforms:21.1:21.1-neoforge:smoke :platforms:21.1:21.1-forge:smoke`. AGENTS.md hard rule 1: the run needs the owner's word.
