---
# modularity-qbqz
title: 'Phase 6: switch MinecraftRuntime, remove utilities, update docs'
status: completed
type: task
priority: high
tags:
    - build
    - docs
created_at: 2026-11-10T09:08:11Z
updated_at: 2026-11-11T23:46:54Z
parent: modularity-km3b
---

Finish the merge.

Tasks:
- [x] MinecraftRuntime uses VersionDetector/PlatformDetector; delete toMcVersion and toLoader
- [x] Delete utilities/ (mvd, mpd, eight projects)
- [x] settings.gradle, root build.gradle (extraJars, :utilities exclusion)
- [x] gradle/platforms.toml: companion-mods, [mod.depends] mvd/mpd, comments
- [x] PlatformTable, AbstractPlatformSetup, AbstractLoomPlatformSetup, AbstractForgeFamilyPlatformSetup
- [x] ModularityRootPlugin extraJars, ModularityVersionPlugin note
- [x] TableAndMetadataTest, SmokeCheck, SmokeCheckTest
- [x] tools/smoke Mod and Server mod lists; tools/smoke-fabric-1.21.1 coordinates
- [x] Docs: 06 renamed and rewritten, 03, 04 link, 07, 00, 08, AGENTS.md, icon-editing
- [ ] Tick modularity-9oz9 and close the epic

Verification (on request): the full command list in the plan.

## Progress

2026-11-10: `MinecraftRuntime` reads `VersionDetector.detect()` and `PlatformDetector.detect().loader()`;
the converters and the four mvd/mpd imports are gone. `utilities/` deleted (59 paths), with the
includes, the `extraJars` block, `companion-mods` in the table, the companion helpers in
`PlatformTable` and the three setups, and the smoke mod lists. `SmokeCheck` and its test expect
`[modularity] platform <LOADER> ...`. All docs listed above are updated; docs/07 §7.10 keeps the
2026-11-06 record and marks the re-run as open. `modularity-9oz9` and the epic stay open until the
runtime checks pass. Gradle tasks were not run (not requested).

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p build-logic test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). `MinecraftRuntime` is switched, `utilities/` is gone and the docs describe one mod.
