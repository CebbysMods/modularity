---
# modularity-7wk5
title: 'forge: optional Forge-only nested artifact for Modularity mixins'
status: scrapped
type: feature
priority: low
tags:
    - port
    - forge
created_at: 2026-11-11T22:54:50Z
updated_at: 2026-11-11T23:47:20Z
---

Route (c) of bean `modularity-6rm0`, not part of the port programme. Forge 1.21.1 has no declarative
mixin route, so this bean is the only way to give Forge Modularity's mixins: a Forge-only nested jar
under `META-INF/jarjar` with its own manifest, added to the launch class path by JarJar, whose launch
plugin calls `Mixins.addConfiguration` with the Forge part's relocated config name. It breaks the
one-universal-jar shape for Forge and it is unproven: no evidence shows that ModLauncher finds a
`ILaunchPluginService` inside a nested jar, or that Mixin applies a config registered that late.

Files: `modularity-gradle/.../UniversalJar.java` (the nested artifact and its manifest), a Forge launch plugin
in `platforms/forge`, `gradle/platforms.toml` only if a new jar task is needed.
Check: the Forge smoke log shows `Mixing` lines for Modularity's targets, and the jar still loads on
Fabric, Quilt and NeoForge unchanged.
Command: `./gradlew -Pmodularity.platforms=forge-21.1 :platforms:21.1:21.1-forge:jar`, then the Forge
smoke against the built jar. The owner's word is needed for the run (AGENTS.md hard rule 1).
Priority: low. The stated limit (PR-13) is the accepted answer for 1.21.1.



Scrapped 2026-11-12: the premise is wrong. A mod jar IS a Mixin container on ModLauncher: Launcher.run
passes the GAME layer's jars to LaunchPluginHandler.offerScanResultsToPlugins, which calls
ILaunchPluginService.addResources, and the Mixin plugin adds each jar as a container resource whose
manifest MixinPlatformAgentDefault reads. The Forge platform jar therefore declares its config with
MixinConfigs, and no Forge-only artifact is needed (docs/07 7.8, bean modularity-6rm0).

## Tracker cleanup 2026-11-12

Route (c) is obsolete. The owner requires mixins on every platform, and the live route is a locator service inside the jar (`modularity-6rm0`), which keeps the one-jar shape. This bean stays scrapped.
