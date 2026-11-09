---
# modularity-2hol
title: Group platform projects under per-version parents
status: completed
type: task
priority: normal
tags:
    - build
    - gradle
created_at: 2026-11-08T18:43:03Z
updated_at: 2026-11-08T18:45:03Z
---

Gradle projects are flat (`:platforms:fabric-21.1`). Group them per Minecraft version so one parent project can aggregate tasks across loaders.

Target layout:

```
:platforms:21.1
:platforms:21.1:21.1-fabric
:platforms:21.1:21.1-quilt
:platforms:21.1:21.1-neoforge
:platforms:21.1:21.1-forge
```

The selection id and published jar name stay `<loader>-<version>` (`fabric-21.1`); only the Gradle project path changes. Parents get `compileAll`, `build`, `smoke` and `genSources` aggregating their children. Adding a version later is purely additive.

## Todos
- [x] PlatformTable: projectName/parentPath/parentDir, path by project name
- [x] Settings plugin: include parent once plus children
- [x] ModularityVersionPlugin with aggregate tasks
- [x] Root plugin applies the version plugin
- [x] Update tests and docs/07
- [x] Verify projects and aggregate tasks, then PR

## Summary of Changes

Platform projects now live under one parent per Minecraft version: `:platforms:21.1` with children `:platforms:21.1:21.1-fabric`, `:21.1-quilt`, `:21.1-neoforge` and `:21.1-forge`. Selection ids and jar names stay `<loader>-<version>`, so `-Pmodularity.platforms=fabric-21.1` is unchanged.

`PlatformTable` gained `projectName()`, `parentPath()`, `parentDir()`, `platformByProjectName()` and `parentPaths()`. A new `ModularityVersionPlugin` adds `compileAll`, `build`, `smoke` and `genSources` to each parent; tasks a child lacks (NeoForge/Forge `genSources`) are skipped.

Verified with `./gradlew projects`, dry-runs of all aggregates, `:platforms:21.1:compileAll`, and `./gradlew -p build-logic test`.
