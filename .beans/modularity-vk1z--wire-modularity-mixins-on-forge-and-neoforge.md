---
# modularity-vk1z
title: Wire Modularity mixins on Forge and NeoForge
status: scrapped
type: task
priority: normal
tags:
    - build
    - mixin
created_at: 2026-11-08T19:47:47Z
updated_at: 2026-11-11T23:47:00Z
---

The 7 mixins (BlockItemMixin, BlockStateBaseMixin, EntityAccessor, LevelMixin, ServerLevelMixin, and the client
CameraMixin, ParticleEngineMixin) are only wired for Fabric/Quilt today: GenerateModMetadataTask deliberately skips
Forge and NeoForge ("not wired (no <loader> runtime yet)"), so those classes ship in the jar unused.

## Scope
- Declare the mixin config in `META-INF/mods.toml` and `META-INF/neoforge.mods.toml` (`[[mixins]] config=...`,
  supported since Forge/NeoForge 1.20.2).
- One mixin config per loader family, listing the classes as compiled for that platform (official names; no refmap
  needed because the runtime namespace is official).
- Make sure Forge/NeoForge's Mixin runtime is present (ForgeGradle/ModDevGradle ship it; verify on a real run).
- Verify with the smoke runs once the production-jar check exists.

## Why it is separate
Deferred from the universal-jar change (2026-11-08) so that work stays on parchment + single jar. The universal jar
ships the Forge/NeoForge halves without mixins until this is done.

## Acceptance
- [ ] mods.toml / neoforge.mods.toml declare the mixin config
- [ ] Mixins apply on Forge and NeoForge in a real game run
- [ ] Smoke checks cover both loaders

## Progress 2026-11-11
NeoForge half in the tree: `GenerateModMetadataTask` writes `modularity.neoforge.mixins.json` and `ModMetadata.modsToml` adds `[[mixins]] config=...`; no refmap, because the NeoForge runtime is in the official namespace. The metadata unit test covers the block and compiles; it did not run. Forge half is open: it needs a refmap and belongs to the Forge port (stage 5). No Gradle run: AGENTS.md hard rule 1. Verification stays open.

Forge half, checked 2026-11-11: ForgeGradle 7.0.40 has no mixin annotation processor and no `reobf` or `mixin` task, so a refmap cannot be produced for Forge 1.21.1. `GenerateModMetadataTask` therefore emits no Forge mixin config and `mods.toml` declares no `[[mixins]]`. The assumption in Scope above ("no refmap needed because the runtime namespace is official") holds for NeoForge and does not hold for Forge, which still needs a refmap. The owner must decide: change the Forge toolchain, or ship Forge without Modularity mixins and state the limit in docs/00 PR-13. Recorded in bean `modularity-6rm0`.

Refinement of the Forge half: the Forge `mods.toml` has no `[[mixins]]` block and FML 52.1.21 references no mixin code, so the earlier plan (declare the config in mods.toml) does not apply to Forge. There, Mixin is a separate library and reads the `MixinConfigs` manifest attribute. The refmap question is open, not settled: no local evidence shows whether the Forge 1.21.1 runtime resolves official names directly. A run with one Forge mixin config decides it. Also blocked: hit particles (`platform/client/HitParticles`) are driven by Modularity mixins, so they do not work on Forge until the mixins apply.

Refmap question settled 2026-11-11 (inspection, no run): neither Forge-family loader needs a refmap. The Forge 1.21.1 distribution jar (`forge-1.21.1-52.1.21.jar` in the mavenizer repo) holds the vanilla classes under their official names — `net/minecraft/client/Camera.class`, `net/minecraft/client/particle/ParticleEngine.class`, `net/minecraft/world/item/BlockItem.class` and `BlockBehaviour$BlockStateBase.class` are all present — so a mixin written against official names resolves as written. The three earlier notes above that say Forge needs a refmap are wrong. The only Forge blocker is the declaration mechanism: `MixinPlatformAgentDefault` reads the `MixinConfigs` manifest attribute, and `UniversalJar.write` gives the universal jar one manifest for all four loaders. The owner still chooses the route.



NeoForge half proved 2026-11-11 (run): with `[[mixins]] config="modularity.neoforge.mixins.json"` in `neoforge.mods.toml`, the NeoForge server and client smokes pass with no attach error, so FML reads the block and Mixin applies the config. The config applies from the development run; the shipped jar is not yet checked (every dev run reports `nested=no`).

Forge half still open and unchanged in shape: no `[[mixins]]` key in `mods.toml`, `MixinPlatformAgentDefault` reads the `MixinConfigs` jar-manifest attribute, and the universal jar has one manifest shared by four loaders. The owner chooses the route (bean `modularity-6rm0`): a Forge-only jar, a loader-specific manifest, or a documented limit in docs/00 PR-13. No refmap work is needed on either loader.

Forge half closed as a loader limit, 2026-11-11 (second evidence pass). Mixin 0.8.7 is active on Forge
(`Service=ModLauncher` in the smoke logs), but none of the six candidate routes can be used by a mod:
the mod jar is never a mixin container, the `MixinConfigs` attribute is read on the game container only,
the `addResource` hooks have no caller, FML 52 coremods are JavaScript, `mixin.config` is a launch option,
and a `META-INF/services` entry works only from the launch class path (development runs only). Full
evidence in bean `modularity-6rm0` and docs/07 §7.8. The two acceptance boxes for Forge stay open; the
NeoForge boxes are met in a development run.



Correction and decision 2026-11-12: the refmap claim in the note above is wrong for Forge. The Forge 1.21.1 distribution jar carries the vanilla classes under official names, so a config written against official names applies as written; no refmap is needed on either Forge-family loader. The real blocker on Forge is the declaration mechanism, and no route is open with one universal jar and mixin 0.8.7. The owner decided (2026-11-12) to state the limit: PR-13 records it, Forge ships without Modularity's mixins, and route (c), a Forge-only nested artifact, is the optional bean `modularity-7wk5`. `modularity-6rm0` is closed as decided.



Update 2026-11-12 (owner reversal): mixins are required on every platform. Forge is now wired —
GenerateModMetadataTask generates modularity.forge.mixins.json and ForgePlatformSetup sets the jar
manifest attribute MixinConfigs (docs/07 7.8, bean modularity-6rm0). The statement above that the
ForgeFamily metadata has no route is obsolete: Forge reads the manifest, not mods.toml. The Forge
smoke has not yet shown the config applying.

Update 2026-11-12 (the proof now exists as a check): the Forge config sets `"verbose": true`, so Mixin writes `Mixing <mixin> from <config>.forge.mixins.json into <target>` at INFO. `SmokeCheck` requires that line for the Forge loader only, and `SmokeCheckTest` holds a case that fails when the line is absent (plus a NeoForge case that passes without it). A green Forge server smoke therefore proves the config applied, not only that no attach error appeared. No run has been made yet.

## Tracker cleanup 2026-11-12

Scrapped as a duplicate, so the tracker has one owner for one piece of work. Its two halves are `modularity-9rzu` (NeoForge, proved in a development run) and `modularity-6rm0` (Forge, open on the locator service). Its Forge acceptance box stays open in `modularity-6rm0`.

The bean body stays as the record.
