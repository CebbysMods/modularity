---
# modularity-xt51
title: 'loom: detection and the log line'
status: completed
type: task
priority: high
created_at: 2026-11-10T21:44:12Z
updated_at: 2026-11-11T23:46:52Z
parent: modularity-q4hh
---

Files: `loom/detect/{ModularityLoomClasses,ModularityLoomPlatformSource,ModularityLoomVersionSource}.java`, `platform/detect/DetectionLog.java`.
Work: audit each source against the Fabric reference and record the result in docs/07 §7.3.
Check: the smoke log holds `platform fabric:<loader>, minecraft:<version>` once.

Audit 2026-11-10: `ModularityLoomVersionSource` and `ModularityLoomPlatformSource` are installed by `ModularityLoomSeams.install()` from both `ModularityLoomCommon.init` and `ModularityLoomLaunch.init`; `ModularityLoomClasses` probes for Fabric Loader and Quilt Loader without loading them. No stub in `loom`. The log line is unproved until a smoke run.



2026-11-10 (branch feature/port-loom): stage 1 audit. The work is in the tree. `grep` finds no `UnsupportedOperationException` and no `notImplemented` under `platforms/loom`. The Fabric root now holds only `fabric/entrypoint` and `fabric/guest`, so each functionality lives in one place. `javac` compiled the loom, fabric and quilt sources against the 1.21.1 mapped jar and the Fabric, Quilt and Allure class paths with no error. No Gradle run (hard rule 1), so every smoke check below stays open: `-p build-logic test`, `-Pmodularity.platforms=fabric-21.1 :platforms:21.1:smoke`, `-Pmodularity.platforms=quilt-21.1 :platforms:21.1:smoke`.
Specific: `ModularityLoomPlatformSource` reports `QUILT` when `org.quiltmc.loader.api.QuiltLoader` is present, and `ModularityLoomVersionSource` reads `FabricLoader`.

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p build-logic test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). The logs hold `(...) platform fabric:0.20.1, minecraft:1.21.1` and `quilt:0.30.2`.
