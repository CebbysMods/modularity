---
# modularity-p999
title: 'loom: guest mods'
status: completed
type: task
priority: normal
created_at: 2026-11-10T21:44:13Z
updated_at: 2026-11-11T23:46:53Z
parent: modularity-q4hh
---

Files: `loom/guest/GuestCandidates.java`, `fabric/guest/{FabricGuestJson,LoaderInjection}.java`.
Work: keep the candidate scan in `loom` and the Fabric Loader injection in `fabric`; state in docs/08 why the injection cannot move.
Check: the smoke log shows `Loaded guest mod smoke_guest` and `Guest mod smoke_guest: loaded=true initialized=true`.

Audit 2026-11-10: `loom/guest/GuestCandidates` scans; `fabric/guest` holds `FabricGuestJson` and `LoaderInjection` (Fabric Loader private fields). Quilt Loader has no such classes, so the injection stays in `fabric` and the Quilt route is stage 2. This decision is now written in docs/07 §7.2.



2026-11-10 (branch feature/port-loom): stage 1 audit. The work is in the tree. `grep` finds no `UnsupportedOperationException` and no `notImplemented` under `platforms/loom`. The Fabric root now holds only `fabric/entrypoint` and `fabric/guest`, so each functionality lives in one place. `javac` compiled the loom, fabric and quilt sources against the 1.21.1 mapped jar and the Fabric, Quilt and Allure class paths with no error. No Gradle run (hard rule 1), so every smoke check below stays open: `-p build-logic test`, `-Pmodularity.platforms=fabric-21.1 :platforms:21.1:smoke`, `-Pmodularity.platforms=quilt-21.1 :platforms:21.1:smoke`.
Specific: `GuestCandidates` scans `mods/` and `-Dmodularity.guestMods`; only the Fabric launch injects (`LoaderInjection`, `FabricGuestJson`). Quilt reports the guests and loads none (docs/08).

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p build-logic test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). The Fabric log holds `Loaded guest mod smoke_guest` and `Guest mod smoke_guest: loaded=true initialized=true`. The shipped-jar half is `modularity-xbj0`.
