---
# modularity-vk1z
title: Wire Modularity mixins on Forge and NeoForge
status: todo
type: task
priority: normal
tags:
    - build
    - mixin
created_at: 2026-11-08T19:47:47Z
updated_at: 2026-11-08T19:47:47Z
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
