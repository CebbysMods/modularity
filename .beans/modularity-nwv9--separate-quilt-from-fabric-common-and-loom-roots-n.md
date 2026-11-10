---
# modularity-nwv9
title: 'Separate Quilt from Fabric: common and loom roots, native Quilt jar'
status: in-progress
type: feature
priority: high
tags:
    - build
    - quilt
created_at: 2026-11-09T15:08:37Z
updated_at: 2026-11-09T15:25:49Z
---

Implements the approved plan (plan file `serene-puzzling-otter.md`).

Goal: stop Quilt from compiling the Fabric layer. Stacks become
`minecraft, common, loom, quilt` (Fabric: `minecraft, common, loom, fabric`).

Done in the working tree:
- `platforms/common` root: `EntryPoint`, `EntryPoints`, `ModularityCommon`, `ModularityServer`,
  `GuestDescriptor`, `LoaderInfo`, `Loaders` moved from `platforms/minecraft`.
- `platforms/loom` root: Fabric-API code and version layers moved from `platforms/fabric`;
  `Loom*` classes, `LoomEntrypoints`, `GuestCandidates`.
- Native Quilt entrypoints `ModularityQuilt{Common,Client,Server,Launch}`; QMJ generation with
  `intermediate_mappings`, group, entrypoints, nested `jars`; per-loader mixin config names.
- Universal jar: fourth (Quilt) part, per-part metadata rewrite; Quilt-only builds no longer fail.
- Smoke: shared sources in `tools/smoke/loom`, `tools/smoke/quilt` QMJ.
- Docs updated.

Open: run `./gradlew -p build-logic test`, the Fabric and Quilt smokes, and the universal jar; then
check the shipped jar (Quilt QMJ + Fabric FMJ in one jar). The NeoForge `ModContext` smoke failure is
separate.


Update 2026-11-09:
- `quilt.mod.json` keeps `jars` inside `quilt_loader` (the schema reader reads it there, not at the root).
- `FabricGuestJson` and `LoaderInjection` are public: the Fabric launch entrypoint is in a different package.
- `Block0` renamed to `BlockBase` in `platform.block.Contexts`.
- `TODO-NOW.md` now has four open checks for this work (Loom QMJ dev run, two metadata files in the
  universal jar, nested jar name, split package). The loader facts and accepted costs moved to
  docs/07 §7.8; refmaps stay in `modularity-dals`.
- Verification runs are still outstanding (not run in this session).


Verified 2026-11-09: `./gradlew -p build-logic test` — BUILD SUCCESSFUL (compileJava, compileTestJava, test executed).
The platform builds and smokes in the Open list still need their own runs.


Verified 2026-11-09:
- `./gradlew -Pmodularity.platforms=quilt-21.1 buildAll` — BUILD SUCCESSFUL, including
  `:build-logic:test` and the universal jar with a Quilt class set.
- `./gradlew -Pmodularity.platforms=quilt-21.1 :platforms:21.1:21.1-quilt:smoke` — BUILD SUCCESSFUL
  (server and client). mpd reports `QUILT 0.30.2`; the four Quilt entrypoints ran; no guest on Quilt.
- Universal jar inspected: `quilt.mod.json` entrypoints under `universal.quilt.quilt.entrypoint`,
  `modularity.quilt.mixins.json`, `META-INF/jars/modularity-api-0.1.0-SNAPSHOT.jar`.

First smoke run failed: Quilt Loader in development read `quilt_loader.jars` from
`build/resources/main` and the nested jar did not exist there (Loom nests it only in the jar task).
Fix: the generated QMJ no longer names nested jars; `QuiltJarMetadata` adds `quilt_loader.jars` to
the built jar from the actual `META-INF/jars/` entries, after Loom nests them. New unit test:
`QuiltJarMetadataTest`.

Open: the shipped universal jar in a real Quilt install (production metadata files in one jar).
