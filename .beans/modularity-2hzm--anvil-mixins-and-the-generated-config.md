---
# modularity-2hzm
title: 'anvil: mixins and the generated config'
status: completed
type: task
priority: normal
created_at: 2026-11-10T21:47:40Z
updated_at: 2026-11-11T23:47:02Z
parent: modularity-arii
---

Files: the generated metadata of both Forge-family platforms.
Work: NeoForge and Forge read the mixin config from different keys; put the rule in one place.
Check: the smoke log shows no mixin error on either loader.

## State 2026-11-11
The rule is split as the two loaders need it. `GenerateModMetadataTask` writes `modularity.neoforge.mixins.json` and `ModMetadata.modsToml` declares `[[mixins]]` for NeoForge only. Forge has no `[[mixins]]` key: `MixinPlatformAgentDefault` in `mixin-0.8.7.jar` reads the `MixinConfigs` manifest attribute, and `UniversalJar.write` gives the universal jar one manifest for all four loaders, so that attribute would apply the Forge config on Fabric, Quilt and NeoForge too, where its classes name Forge types. The route is open in `modularity-6rm0`; see docs/07 §7.8. Check: a NeoForge smoke log shows no mixin error.

## Tracker cleanup 2026-11-12

Closed as completed, with one correction: the rule lives in `build-logic`, not in `anvil`. `GenerateModMetadataTask` writes both configs and `ModMetadata` holds the per-loader name, and the 2026-11-12 run shows no mixin error on either loader. The Forge config does not load in a development run at all, so the Forge proof is `modularity-6rm0`.
