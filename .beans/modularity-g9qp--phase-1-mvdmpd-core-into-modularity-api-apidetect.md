---
# modularity-g9qp
title: 'Phase 1: mvd/mpd core into modularity-api (api.detect)'
status: completed
type: task
priority: high
tags:
    - architecture
    - api
created_at: 2026-11-10T09:08:03Z
updated_at: 2026-11-11T23:46:54Z
parent: modularity-km3b
---

Add lv.cebbys.mcmods.modularity.api.detect to modularity-api and retire the duplicate version model.

Tasks:
- [x] api.core.Version: sealed base (major/minor/patch/raw, compareTo rejects cross-kind)
- [x] api.core.MinecraftVersion: rename of McVersion, extends Version
- [x] api.core.PlatformVersion: lenient loader version, unknown()
- [x] api.detect: VersionSource, PlatformSource, DetectedPlatform, VersionDetector, PlatformDetector
- [x] api.detect: VersionJsonSource, ReflectionSource, MarkerClassSource
- [x] api.detect: VersionDetectionException, PlatformDetectionException
- [x] Replace ServiceLoader with install(...); built-ins always present
- [x] Rename McRuntime to MinecraftRuntime (McModContext, ModularityCommon)
- [x] Move the mvd/mpd-core tests and marker stubs; add Version/PlatformVersion tests
- [x] modularity-api/build.gradle with the JUnit test block
- [x] docs/03-api-design.md package table

Verification (on request): :modularity-api:test, :modularity-testkit:test, -p build-logic test.

## Progress

- 2026-11-10: `api.core.Version`, `MinecraftVersion`, `PlatformVersion` added; `McVersion` removed and
  its users in `api`, `modularity-testkit` and `platform.MinecraftRuntime` renamed. `api.detect`
  package created with explicit `install(...)` and built-in sources; `ServiceLoader` and
  `DetectedVersion` removed. `McRuntime` and `McModContext` renamed to `MinecraftRuntime` and
  `MinecraftModContext`. Tests moved to `modularity-api/src/test` with marker stubs; 28 tests pass
  through a direct JUnit launcher. `javac --release 17` compiles the API, the testkit and the API
  tests. Gradle tasks were not run (not requested).

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p build-logic test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). `api.detect` is in `modularity-api`, `utilities/` is gone and all four loaders report the new detection line.
