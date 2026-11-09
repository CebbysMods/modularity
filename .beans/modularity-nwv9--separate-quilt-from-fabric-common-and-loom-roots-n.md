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
updated_at: 2026-11-09T15:08:37Z
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
