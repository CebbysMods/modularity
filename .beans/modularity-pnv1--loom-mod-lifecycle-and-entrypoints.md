---
# modularity-pnv1
title: 'loom: mod lifecycle and entrypoints'
status: completed
type: task
priority: high
created_at: 2026-11-10T21:44:12Z
updated_at: 2026-11-11T23:46:52Z
parent: modularity-q4hh
---

Files: `loom/entrypoint/ModularityLoomEntrypoints.java`; the four one-line entrypoints in `fabric/entrypoint` and `quilt/entrypoint`.
Work: split `ModularityLoomEntrypoints` into `ModularityLoomCommon`, `ModularityLoomClient`, `ModularityLoomServer` and `ModularityLoomLaunch` (bean modularity-xniw), so each loader entrypoint calls one class that does one stage.
Check: `-p build-logic test`, then the 21.1 Fabric smoke shows the four stage log lines in order.

2026-11-10: the class split is in the working tree on branch feature/port-loom (see modularity-xniw); javac compiles the three roots. The smoke check waits for a Gradle run.



2026-11-10 (branch feature/port-loom): stage 1 audit. The work is in the tree. `grep` finds no `UnsupportedOperationException` and no `notImplemented` under `platforms/loom`. The Fabric root now holds only `fabric/entrypoint` and `fabric/guest`, so each functionality lives in one place. `javac` compiled the loom, fabric and quilt sources against the 1.21.1 mapped jar and the Fabric, Quilt and Allure class paths with no error. No Gradle run (hard rule 1), so every smoke check below stays open: `-p build-logic test`, `-Pmodularity.platforms=fabric-21.1 :platforms:21.1:smoke`, `-Pmodularity.platforms=quilt-21.1 :platforms:21.1:smoke`.
Specific: `ModularityLoomCommon`, `ModularityLoomClient`, `ModularityLoomServer` and `ModularityLoomLaunch` replace `ModularityLoomEntrypoints`; the eight Fabric and eight Quilt entrypoints call the matching class.

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p build-logic test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). The Fabric logs hold the launch, common and side stage lines in stage order.
