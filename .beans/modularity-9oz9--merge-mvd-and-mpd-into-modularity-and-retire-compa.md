---
# modularity-9oz9
title: Merge mvd and mpd into Modularity and retire companion-mods
status: todo
type: feature
priority: normal
tags:
    - build
    - architecture
created_at: 2026-11-09T11:58:06Z
updated_at: 2026-11-10T09:08:14Z
blocked_by:
    - modularity-km3b
---

`mvd` (Minecraft Version Detector) and `mpd` (Minecraft Platform Detector) exist only so Modularity can
learn which Minecraft version and loader it runs on. They ship as separate mod ids with their own jars,
their own version numbers and eight Gradle projects (core + fabric + neoforge + forge per utility). The
platform setups only exist to wire them in: `companion-mods` in `gradle/platforms.toml`, and the
`companionProject` / `companionApi` derivation shared by both platform bases.

Goal: research how to move the detection into Modularity itself, then implement it; `companion-mods`
becomes a deprecated seam that is removed in the same change. This is its own feature, not part of the
platform-setup refactor.

## Research

- Where does each piece belong? The detection splits naturally into:
  - version-independent logic and the SPI (`VersionDetector`, `DetectedVersion`, `VersionSource`,
    `PlatformDetector`, `DetectedPlatform`, `PlatformSource`, marker-class and `version.json` sources) —
    plain Java, no Minecraft or loader dependency;
  - loader-specific sources (`FabricVersionSource`, `FabricPlatformSource`, NeoForge/Forge equivalents) —
    compiled against that loader.
  Map both onto the layer stack (`platforms/minecraft/common`, `platforms/<loader>/common`,
  `versions/<version>`) and decide whether the pure part goes into `modularity-api` (currently
  dependency-free and nested) or into a layer of its own. Check what each choice does to the universal
  jar's relocation (`universal/<loader>`) and to `McRuntime`.
- Is `ServiceLoader` still worth it once everything is in one jar and one class loader? Compare keeping
  the SPI (still useful for the loader-specific sources, which cannot be compiled into the shared
  layers) with a small factory per loader. Prefer removing the `META-INF/services` duplicate-file
  workaround if the split disappears.
- Version and platform information must stay correct in dev runs *and* from the shipped universal jar,
  on all four loaders, including Quilt (reported as QUILT through Fabric compatibility).
- Startup order: today the detectors run in their own mod entry points before Modularity's. Work out
  where detection moves to and whether anything depends on the current load order or on the
  `[mvd]` / `[mpd]` log lines.
- Decide the public surface: if detection becomes internal, `modularity-api` need not expose it; if
  guest mods or third parties may want it, say so and keep it.

## Removal checklist (what "deprecated and removed together" touches)

- [x] `gradle/platforms.toml`: drop `companion-mods`, and the `mvd`/`mpd` entries under `[mod.depends]`
      if they only exist for the companion jars. Update the comment that points at
      `PlatformTable.Mod.companionProject`.
- [x] `PlatformTable`: remove the `companionMods` component from `Mod`, its parsing, and the static
      `companionProject` / `companionApi` helpers.
- [x] `AbstractPlatformSetup.companionProject` (the fail-fast project check) and the companion blocks
      in `AbstractLoomPlatformSetup` and `AbstractForgeFamilyPlatformSetup`. `ModularityRootPlugin`
      also lost `extraJars`.
- [x] `TableAndMetadataTest`: the fixture's `companion-mods` and the two derivation assertions.
- [x] Generated metadata: `ModMetadata` / `GenerateModMetadataTask` must stop writing `"mvd"` / `"mpd"`
      into `fabric.mod.json`, `mods.toml` and `neoforge.mods.toml` `"depends"` sections.
- [x] Delete `utilities/mvd/{mvd-core,mvd-fabric,mvd-neoforge,mvd-forge}` and `utilities/mpd/...`
      (eight projects), their `gradle.properties` files, their `settings.gradle` includes, their
      publishing configuration, and the root `collectJars` step that copies three jars.
- [x] Smoke: `SmokeCheck` expectations (`[mvd]` / `[mpd]` lines, loaded-mod counts), `SmokeServer`'s
      three-mod log, and any `tools/smoke` assumption that both utilities are separate mods.
- [x] Docs: `docs/06-utility-mods.md` (the whole document), `docs/07-multi-version-build.md` §7.5,
      `docs/08` where guest/companion mods are described, and the root README if it lists the mods.
- [x] Check other beans for references to mvd/mpd (the parchment bean names mvd in its verification
      notes) and update them rather than leaving stale instructions.

## Acceptance

- Modularity detects version, loader and side in development and from the universal jar on Fabric,
  Quilt, NeoForge and Forge, with no separate detector mod installed.
- The build produces and publishes the Modularity jars only; `utilities/mvd` and `utilities/mpd` are
  gone; `companion-mods` no longer appears in the table, the code or the metadata.
- Smoke tests for all four loaders pass without the detector mods, and their checks assert the
  detection results through Modularity's own log lines.
- Docs describe one mod, not three.
