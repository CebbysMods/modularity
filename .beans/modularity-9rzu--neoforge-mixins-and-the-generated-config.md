---
# modularity-9rzu
title: 'neoforge: mixins and the generated config'
status: completed
type: task
priority: normal
created_at: 2026-11-10T21:47:41Z
updated_at: 2026-11-11T23:46:53Z
parent: modularity-yrjc
---

Files: the generated NeoForge metadata.
Work: audit that the config name and the mixin package match, and that client mixins load on the client only.
Check: the smoke log shows no mixin error.

## Audit 2026-11-11 (stage 4)
In the tree: `GenerateModMetadataTask` writes the mixin config for NeoForge and `ModMetadata.modsToml` adds `[[mixins]] config="modularity.neoforge.mixins.json"`. NeoForge runs in the official namespace, so no refmap is written. `ModMetadata.findMixins` lists the shared `@Mixin` classes as before.
## Open risk
A real NeoForge run must show FML applying the config; the mixin classes ship unused until then. Forge stays on the old path (stage 5) because it needs a refmap. No Gradle run: AGENTS.md hard rule 1. Verification stays open in the verification bean.

Correction 2026-11-11: the note that "Forge stays on the old path because it needs a refmap" is wrong. Neither Forge-family loader needs a refmap; the Forge 1.21.1 distribution jar runs in the official namespace. Forge stays unwired because its `mods.toml` has no `[[mixins]]` block and the shared manifest blocks the attribute route (bean `modularity-6rm0`).



## Proof 2026-11-11
The NeoForge server and client smokes pass, and neither log holds an attach error, so FML reads `[[mixins]] config="modularity.neoforge.mixins.json"` and Mixin applies the config. The failing run had one cause, and it was in the build: a development run could not see the API (ModDevGradle leaves project dependencies off the launch class path), so the mixins, written against the API, failed at attach time. Fix: `NeoForgePlatformSetup.addNestedModulesToRun` adds the nested modules to each run's additional runtime class path. Recorded in docs/07 §7.8.
The shared layer is unchanged: `Interceptors` and the two mixins are restored to their committed form, and the Fabric, Quilt, NeoForge and Forge smokes pass again with them.
The remaining open part is the shipped jar: every dev run reports `nested=no`, so the jar-in-jar path is unproven.

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p build-logic test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). The NeoForge log holds no mixin error. The shipped-jar half of the mixin area is `modularity-xbj0`.
